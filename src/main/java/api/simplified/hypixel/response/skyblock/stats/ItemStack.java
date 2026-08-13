package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.model.Accessory;
import api.simplified.skyblock.model.BonusItemStat;
import api.simplified.skyblock.model.BonusReforgeStat;
import api.simplified.skyblock.model.Enchantment;
import api.simplified.skyblock.model.Gemstone;
import api.simplified.skyblock.model.HotPotatoStat;
import api.simplified.skyblock.model.Item;
import api.simplified.skyblock.model.Reforge;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.util.StringUtil;
import lib.minecraft.nbt.NbtFactory;
import lib.minecraft.nbt.exception.NbtException;
import lib.minecraft.nbt.tag.ByteArrayTag;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.IntTag;
import lib.minecraft.nbt.tag.ListTag;
import lib.minecraft.nbt.tag.NumericalTag;
import lib.minecraft.nbt.tag.StringTag;
import lib.minecraft.nbt.tag.Tag;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One item instance, read out of its NBT tag and paired with the reference data it names.
 * <p>
 * An item on the wire is a tag tree, not a stat sheet - the modifiers that decide what it actually
 * gives sit under {@code tag.ExtraAttributes} as ids, and mean nothing until each is looked up. The
 * constructor does that lookup once and totals everything unconditional, so what is left is only
 * whatever depends on the rest of the player.
 * <p>
 * An accessory is an item, so it is one of these too - the buckets it never fills simply stay
 * unwritten, and the ones an armour piece never fills do the same. Anything conditional on the rest
 * of the player is left to the bonus pass, which needs totals this cannot see.
 */
@Getter
public final class ItemStack extends StatData<ItemOrigin> {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("M/d/yy h:m a", Locale.US);
    private static final ZoneId HYPIXEL_TIMEZONE = ZoneId.of("America/New_York");

    /**
     * Reference data for the item this instance is of.
     */
    private final @NotNull Item item;

    /**
     * Reference data for the accessory this instance is of, empty for anything not out of the bag.
     */
    private final @NotNull Optional<Accessory> accessory;

    /**
     * The item's NBT tag exactly as it was decoded, still holding everything not read out here.
     */
    private final @NotNull CompoundTag compoundTag;

    /**
     * Rarity after every upgrade the instance carries, which may sit above the item's own.
     */
    private final @NotNull Rarity rarity;

    /**
     * Conditional stat bonuses declared for this item, still to be evaluated.
     */
    private final @NotNull ConcurrentList<BonusItemStat> bonusItemStatModels;

    /**
     * Reforge applied to the instance, empty when it carries none.
     */
    private final @NotNull Optional<Reforge> reforge;

    /**
     * Conditional bonus declared for the applied reforge, empty when it has none or none is applied.
     */
    private final @NotNull Optional<BonusReforgeStat> bonusReforgeStatModel;

    /**
     * Quality of each gemstone slotted into the instance, keyed by the gemstone.
     */
    private final @NotNull ConcurrentMap<Gemstone, ConcurrentList<Gemstone.Type>> gemstones;

    /**
     * Level of each enchantment on the item, keyed by the enchantment.
     */
    private final @NotNull ConcurrentMap<Enchantment, Integer> enchantments;

    /**
     * Stat substitutes each enchantment qualifies for at its level, including the mob-specific ones
     * left out of the totals.
     */
    private final @NotNull ConcurrentMap<Enchantment, ConcurrentList<Stat.Substitute>> enchantmentStats;

    /**
     * Stat the instance has been enriched toward, empty when it carries no enrichment.
     */
    private final @NotNull Optional<Stat> enrichmentStat;

    /**
     * Hot potato books applied, counting both hot potato books and fuming potato books.
     */
    private final int hotPotatoBooks;

    /**
     * Whether a recombobulator has raised the instance one rarity.
     */
    private final boolean recombobulated;

    /**
     * Whether a tier boost has raised the instance one rarity.
     */
    private final boolean tierBoosted;

    /**
     * When the instance was obtained, empty for anything old enough to predate the stamp.
     */
    private final @NotNull Optional<Long> timestamp;

    @Getter(AccessLevel.NONE) private final boolean hasArtOfWar;
    @Getter(AccessLevel.NONE) private final boolean hasArtOfPeace;

    /**
     * Constructs a new {@code ItemStack} and totals every unconditional modifier on the instance.
     *
     * @param itemModel reference data for the item being read
     * @param accessory reference data for the accessory, empty for anything not out of the bag
     * @param compoundTag the item's NBT tag
     */
    public ItemStack(@NotNull Item itemModel, @NotNull Optional<Accessory> accessory, @NotNull CompoundTag compoundTag) {
        this.item = itemModel;
        this.accessory = accessory;
        this.compoundTag = compoundTag;
        this.timestamp = readTimestamp(compoundTag.getPath("tag.ExtraAttributes.timestamp"));
        this.recombobulated = RarityUpgrade.isRecombobulated(compoundTag);
        this.tierBoosted = RarityUpgrade.isTierBoosted(compoundTag);
        this.rarity = RarityUpgrade.resolve(itemModel, compoundTag);
        this.hotPotatoBooks = compoundTag.getPathOrDefault("tag.ExtraAttributes.hot_potato_count", IntTag.EMPTY).getValue();
        this.hasArtOfWar = compoundTag.containsPath("tag.ExtraAttributes.art_of_war_count");
        this.hasArtOfPeace = compoundTag.containsPath("tag.ExtraAttributes.artOfPeaceApplied");

        // Load Gemstones
        CompoundTag gemTag = compoundTag.getPathOrDefault("tag.ExtraAttributes.gems", CompoundTag.EMPTY);
        this.gemstones = Concurrent.newUnmodifiableMap(gemTag.notEmpty() ? findGemstones(SkyBlockData.getRepository(Gemstone.class).findAll(), gemTag) : Concurrent.newMap());

        // Load Bonus Item Stat Model
        this.bonusItemStatModels = SkyBlockData.getRepository(BonusItemStat.class)
            .findAll(BonusItemStat::getItemId, itemModel.getId())
            .collect(Concurrent.toUnmodifiableList());

        // Load Reforge Model
        // an accessory slot never contributes a reforge, so it never resolves one either - the field
        // stays because an accessory really can hold one, what goes is the lookup and the write
        this.reforge = accessory.isPresent()
            ? Optional.empty()
            : SkyBlockData.getRepository(Reforge.class).findFirst(Reforge::getId, compoundTag
                .getPathOrDefault("tag.ExtraAttributes.modifier", StringTag.EMPTY)
                .getValue()
                .toUpperCase()
            );

        // Load Bonus Reforge Model
        this.bonusReforgeStatModel = this.getReforge().flatMap(reforge -> SkyBlockData.getRepository(BonusReforgeStat.class).findFirst(BonusReforgeStat::getReforgeId, reforge.getId()));

        // Load Enrichment - look up stat by enrichment key from NBT
        this.enrichmentStat = SkyBlockData.getRepository(Stat.class).findFirst(Stat::getId, compoundTag
            .getPathOrDefault("tag.ExtraAttributes.talisman_enrichment", StringTag.EMPTY)
            .getValue()
            .toUpperCase()
        );

        // Save Stats
        itemModel.getStats().forEach((key, value) -> this.table.add(ItemOrigin.STATS, key, StatHalf.BONUS, value));

        // Save Reforge Stats
        if (accessory.isEmpty()) {
            handleReforgeBonus(this.getReforge(), this.getRarity())
                .forEach((statModel, value) -> this.table.add(ItemOrigin.REFORGES, statModel, StatHalf.BONUS, value));
        }

        // Save Gemstone Stats
        handleGemstoneBonus(this.getGemstones(), this.getRarity())
            .forEach((statModel, value) -> this.table.add(ItemOrigin.GEMSTONES, statModel, StatHalf.BONUS, value));

        // Save Enrichment Stats
        this.getEnrichmentStat()
            .filter(stat -> stat.getEnrichment() > 0.0)
            .ifPresent(stat -> this.table.add(ItemOrigin.ENRICHMENTS, stat, StatHalf.BONUS, stat.getEnrichment()));

        // Save Hot Potato Book Stats
        SkyBlockData.getRepository(HotPotatoStat.class)
            .stream()
            .filter(hotPotatoStat -> hotPotatoStat.getItemTypes().contains(itemModel.getCategory().getId()))
            .forEach(hotPotatoStat -> this.table.add(ItemOrigin.HOT_POTATOES, hotPotatoStat.getStat(), StatHalf.BONUS, this.getHotPotatoBooks() * hotPotatoStat.getValue()));

        // Save Art Of Peace Stats
        if (this.hasArtOfPeace())
            this.table.add(ItemOrigin.SUN_TZU, "HEALTH", StatHalf.BONUS, 40.0);

        // Save Art Of War Stats
        if (this.hasArtOfWar())
            this.table.add(ItemOrigin.SUN_TZU, "STRENGTH", StatHalf.BONUS, 5.0);

        // Save Enchantment Stats
        ConcurrentMap<Enchantment, Integer> enchantments = Concurrent.newMap();
        ConcurrentMap<Enchantment, ConcurrentList<Stat.Substitute>> enchantmentStats = Concurrent.newMap();

        compoundTag.getPathOrDefault("tag.ExtraAttributes.enchantments", CompoundTag.EMPTY)
            .entrySet()
            .stream()
            .map(entry -> Pair.of(
                SkyBlockData.getRepository(Enchantment.class).findFirst(Enchantment::getId, entry.getKey().toUpperCase()).orElse(null),
                ((IntTag) entry.getValue()).getValue()
            ))
            .filter(enchantmentData -> Objects.nonNull(enchantmentData.left()))
            .forEach(enchantmentData -> {
                Enchantment enchantment = enchantmentData.getKey();
                int level = enchantmentData.getValue();

                enchantments.put(enchantment, level);
                enchantmentStats.put(enchantment, Concurrent.newList());

                // Handle Enchantment Stat Substitutes
                enchantment.getStats()
                    .stream()
                    .filter(sub -> sub.getValues().keySet().stream().anyMatch(l -> level >= l))
                    .forEach(sub -> enchantmentStats.get(enchantment).add(sub));

                // Handle Enchantment Stats
                if (enchantment.getMobTypeIds().isEmpty()) {
                    enchantmentStats.get(enchantment)
                        .stream()
                        .filter(sub -> sub.getType() != Stat.Type.PERCENT && sub.getType() != Stat.Type.PLUS_PERCENT) // Static Only
                        .filter(sub -> sub.getStat().isPresent()) // Has Stat
                        .forEach(sub -> {
                            double enchantBonus = sub.getValues().entrySet().stream()
                                .filter(e -> level >= e.getKey())
                                .mapToDouble(Map.Entry::getValue)
                                .sum();
                            this.table.add(ItemOrigin.ENCHANTS, sub.getStat().get(), StatHalf.BONUS, enchantBonus);
                        });
                }
            });

        this.enchantments = enchantments;
        this.enchantmentStats = enchantmentStats;

        // New Year Cake Bag
        if ("NEW_YEAR_CAKE_BAG".equals(itemModel.getId())) {
            try {
                byte[] nbtCakeBag = compoundTag.getPathOrDefault("tag.ExtraAttributes.new_year_cake_bag_data", ByteArrayTag.EMPTY).getValue();
                ListTag<CompoundTag> cakeBagItems = NbtFactory.fromByteArray(nbtCakeBag).getListTag("i");
                this.table.add(ItemOrigin.CAKE_BAG, "HEALTH", StatHalf.BONUS, cakeBagItems.size());
            } catch (NbtException ignore) { }
        }
    }

    @Override
    protected ItemOrigin[] getAllTypes() {
        return ItemOrigin.values();
    }

    /**
     * Whether The Art of Peace has been applied, adding a flat forty health.
     */
    public boolean hasArtOfPeace() {
        return this.hasArtOfPeace;
    }

    /**
     * Whether The Art of War has been applied, adding a flat five strength.
     */
    public boolean hasArtOfWar() {
        return this.hasArtOfWar;
    }

    /**
     * Totals what a set of slotted gemstones give, at the rarity of what they are slotted into.
     * <p>
     * A gemstone's value depends on both its quality and the rarity of what it is slotted into, so
     * the same gemstone is worth more in a legendary item than in a rare one.
     *
     * @param gemstones the quality of each slotted gemstone, keyed by the gemstone
     * @param rarity the rarity the gemstones are scaled against
     * @return the value each stat gains, keyed by the stat
     */
    private static ConcurrentMap<Stat, Double> handleGemstoneBonus(@NotNull ConcurrentMap<Gemstone, ConcurrentList<Gemstone.Type>> gemstones, @NotNull Rarity rarity) {
        ConcurrentMap<Stat, Double> gemstoneAdjusted = Concurrent.newMap();

        gemstones.forEach((gemstone, gemstoneTypes) -> gemstoneTypes.forEach(gemstoneType -> {
            double value = gemstone.getValues()
                .getOrDefault(gemstoneType, Concurrent.newMap())
                .getOrDefault(rarity, 0.0);

            if (value > 0.0)
                gemstoneAdjusted.put(gemstone.getStat(), value + gemstoneAdjusted.getOrDefault(gemstone.getStat(), 0.0));
        }));

        return gemstoneAdjusted;
    }

    /**
     * Totals what a reforge gives at a given rarity.
     *
     * @param optionalReforge the applied reforge, empty when none is
     * @param rarity the rarity the reforge is scaled against
     * @return the value each stat gains, empty when no reforge is applied
     */
    private static ConcurrentMap<Stat, Double> handleReforgeBonus(@NotNull Optional<Reforge> optionalReforge, @NotNull Rarity rarity) {
        ConcurrentMap<Stat, Double> reforgeBonuses = Concurrent.newMap();

        optionalReforge.ifPresent(reforge -> reforge.getStats(rarity)
            .forEach(substitute -> substitute.getStat()
                .ifPresent(stat -> reforgeBonuses.put(stat, substitute.getValues().get(rarity) + reforgeBonuses.getOrDefault(stat, 0.0)))));

        return reforgeBonuses;
    }

    /**
     * Reads when an instance was obtained out of whichever shape its tag carries.
     * <p>
     * The wire spells this two ways: a numeric tag is epoch milliseconds already, and a string tag is
     * the game's own {@code M/d/yy h:m a} rendering in Hypixel's timezone. Anything else, and
     * anything unparseable, is an item old enough to predate the stamp.
     *
     * @param timestampTag the tag under {@code tag.ExtraAttributes.timestamp}, null when absent
     * @return the epoch milliseconds, empty when the tag carries no readable stamp
     */
    private static Optional<Long> readTimestamp(Tag<?> timestampTag) {
        if (timestampTag instanceof NumericalTag<?> numericalTag)
            return Optional.of(numericalTag.getValue().longValue());

        if (!(timestampTag instanceof StringTag stringTag))
            return Optional.empty();

        return Optional.ofNullable(StringUtil.defaultIfEmpty(stringTag.getValue(), null))
            .map(timestamp -> LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT))
            .map(localDateTime -> localDateTime.atZone(HYPIXEL_TIMEZONE))
            .map(ZonedDateTime::toInstant)
            .map(Instant::toEpochMilli);
    }

    private static ConcurrentMap<Gemstone, ConcurrentList<Gemstone.Type>> findGemstones(ConcurrentList<Gemstone> gemstoneModels, CompoundTag gemTag) {
        ConcurrentMap<Gemstone, ConcurrentList<Gemstone.Type>> gemstones = Concurrent.newMap();

        for (Map.Entry<String, Tag<?>> entry : gemTag.entrySet()) {
            for (Gemstone gemstone : gemstoneModels) {
                boolean handle = false;
                String typeKey = null;

                // Handle Specific Slots
                if (entry.getKey().startsWith(gemstone.getId())) {
                    handle = true;
                    typeKey = ((StringTag) entry.getValue()).getValue();
                }

                // Handle Generic Slots
                if (entry.getValue().getValue().equals(gemstone.getId()) && entry.getKey().endsWith("_gem")) {
                    handle = true;
                    typeKey = gemTag.getOrDefault(entry.getKey().replace("_gem", ""), StringTag.EMPTY).getValue();
                }

                if (handle && typeKey != null) {
                    // Populate New Gemstone
                    gemstones.putIfAbsent(gemstone, Concurrent.newList());

                    // Add Gemstone Type
                    try {
                        Gemstone.Type gemType = Gemstone.Type.valueOf(typeKey);
                        gemstones.get(gemstone).add(gemType);
                    } catch (IllegalArgumentException ignore) { }
                }
            }
        }

        return gemstones;
    }

}
