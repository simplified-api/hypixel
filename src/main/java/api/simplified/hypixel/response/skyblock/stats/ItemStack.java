package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.stats.buff.BuffEvaluator;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.model.Accessory;
import api.simplified.skyblock.model.Buff;
import api.simplified.skyblock.model.Enchantment;
import api.simplified.skyblock.model.Gemstone;
import api.simplified.skyblock.model.Item;
import api.simplified.skyblock.model.Reforge;
import api.simplified.skyblock.model.Stat;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
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
 * constructor does that lookup once, and nothing more: an instance is what a slot holds, so the cells
 * belong to the slot's table and {@link #fill} is what puts them there.
 * <p>
 * An accessory is an item, so it is one of these too - the buckets it never fills simply stay
 * unwritten, and the ones an armour piece never fills do the same. Anything conditional on the rest
 * of the player is left to the bonus pass, which needs totals this cannot see.
 */
@Getter
public final class ItemStack {

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
     * Reforge applied to the instance, empty when it carries none.
     */
    private final @NotNull Optional<Reforge> reforge;

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
     * When the instance was obtained, empty for anything old enough to predate the stamp.
     */
    private final @NotNull Optional<Long> timestamp;

    /**
     * Cakes stored in the New Year Cake Bag, empty for anything that is not a readable one.
     */
    private final @NotNull Optional<Integer> newYearCakes;

    @Getter(AccessLevel.NONE) private final boolean hasArtOfWar;
    @Getter(AccessLevel.NONE) private final boolean hasArtOfPeace;

    /**
     * Constructs a new {@code ItemStack} and resolves every modifier the instance names.
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
        this.rarity = resolveRarity(itemModel, compoundTag);
        this.hasArtOfWar = compoundTag.containsPath("tag.ExtraAttributes.art_of_war_count");
        this.hasArtOfPeace = compoundTag.containsPath("tag.ExtraAttributes.artOfPeaceApplied");

        // Load Gemstones
        CompoundTag gemTag = compoundTag.getPathOrDefault("tag.ExtraAttributes.gems", CompoundTag.EMPTY);
        this.gemstones = Concurrent.newUnmodifiableMap(gemTag.notEmpty() ? findGemstones(SkyBlockData.getRepository(Gemstone.class).findAll(), gemTag) : Concurrent.newMap());

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

        // Load Enrichment - look up stat by enrichment key from NBT
        this.enrichmentStat = SkyBlockData.getRepository(Stat.class).findFirst(Stat::getId, compoundTag
            .getPathOrDefault("tag.ExtraAttributes.talisman_enrichment", StringTag.EMPTY)
            .getValue()
            .toUpperCase()
        );

        // Load Enchantments
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
            });

        this.enchantments = enchantments;
        this.enchantmentStats = enchantmentStats;
        this.newYearCakes = readNewYearCakes(itemModel, compoundTag);
    }

    /**
     * Writes everything the instance gives unconditionally into the slot it fills.
     * <p>
     * Nothing here resolves anything - every part was read in the constructor, and what this decides
     * is only which bucket each part lands in. What is conditional on the rest of the player is left
     * to the bonus pass, which needs totals no slot can see.
     *
     * @param kind the kind of slot being filled, which is what decides whether a reforge contributes
     * @param sink the write face of that slot's table
     */
    void fill(@NotNull ItemSlot.Kind kind, @NotNull StatSink sink) {
        // Save Stats
        this.item.getStats().forEach((statId, value) -> sink.add(ItemOrigin.STATS, statId, StatHalf.BONUS, value));

        // Save Reforge Stats
        // an accessory slot writes seven of the eight buckets - the instance may carry a reforge, but
        // the slot it sits in is what decides whether one contributes
        if (kind != ItemSlot.Kind.ACCESSORY) {
            handleReforgeBonus(this.reforge, this.rarity)
                .forEach((statModel, value) -> sink.add(ItemOrigin.REFORGES, statModel, StatHalf.BONUS, value));
        }

        // Save Gemstone Stats
        handleGemstoneBonus(this.gemstones, this.rarity)
            .forEach((statModel, value) -> sink.add(ItemOrigin.GEMSTONES, statModel, StatHalf.BONUS, value));

        // Save Enrichment Stats
        this.enrichmentStat
            .filter(stat -> stat.getEnrichment() > 0.0)
            .ifPresent(stat -> sink.add(ItemOrigin.ENRICHMENTS, stat, StatHalf.BONUS, stat.getEnrichment()));

        // Save Art Of Peace Stats
        if (this.hasArtOfPeace)
            sink.add(ItemOrigin.SUN_TZU, "HEALTH", StatHalf.BONUS, 40.0);

        // Save Art Of War Stats
        if (this.hasArtOfWar)
            sink.add(ItemOrigin.SUN_TZU, "STRENGTH", StatHalf.BONUS, 5.0);

        // Save Enchantment Stats
        this.enchantments.forEach((enchantment, level) -> {
            if (!enchantment.getMobTypeIds().isEmpty()) // Applies Against Anything Only
                return;

            this.enchantmentStats.get(enchantment)
                .stream()
                .filter(sub -> sub.getType() != Stat.Type.PERCENT && sub.getType() != Stat.Type.PLUS_PERCENT) // Static Only
                .filter(sub -> sub.getStat().isPresent()) // Has Stat
                .forEach(sub -> sink.add(ItemOrigin.ENCHANTS, sub.getStat().get(), StatHalf.BONUS, valueAt(sub, level)));
        });

        // Save New Year Cake Bag Stats
        this.newYearCakes.ifPresent(cakes -> sink.add(ItemOrigin.CAKE_BAG, "HEALTH", StatHalf.BONUS, cakes));
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
     * What one substitute is worth at a level.
     *
     * <p>
     * A {@code values} entry is the <b>total at that level</b> rather than what the level adds on top
     * of the one below, so the answer is the entry at the highest key the level has reached and never
     * a sum of the entries below it. Respiration III grants 45 and its map reads
     * {@code {1:15, 2:30, 3:45}}; the ladder is already cumulative and summing it counts every rung
     * again.
     *
     * @param substitute the substitute to read
     * @param level the level the instance carries the enchantment at
     * @return the value at the reached rung, zero when the level reaches none of them
     */
    static double valueAt(@NotNull Stat.Substitute substitute, int level) {
        return substitute.getValues()
            .entrySet()
            .stream()
            .filter(entry -> level >= entry.getKey())
            .max(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .orElse(0.0);
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

    /**
     * The rarity an item instance ends up at, once every upgrade it carries has been folded on.
     *
     * <p>
     * It reads the item's own tag and the rarity rows, and nothing else - no stat total exists yet
     * and none is needed - which is what lets an accessory be counted and ranked without resolving a
     * single stat. That is also why this is a static a caller with a tag and a row can reach rather
     * than something only a built instance answers.
     *
     * @param itemModel reference data for the item being read
     * @param compoundTag the item's NBT tag
     * @return the resolved rarity, the item's own when nothing raises it
     */
    public static @NotNull Rarity resolveRarity(@NotNull Item itemModel, @NotNull CompoundTag compoundTag) {
        BuffEvaluator.Context context = BuffEvaluator.Context.of(Concurrent.newMap())
            .carrier(Buff.Term.Carrier.ID, itemModel.getId())
            .carrier(Buff.Term.Carrier.CATEGORY, itemModel.getCategory().getId())
            .tag(compoundTag);

        double ordinal = itemModel.getRarity().ordinal();

        for (Buff row : BuffEvaluator.select(Buff.Subject.Kind.ITEM, itemModel.getId(), null))
            ordinal = BuffEvaluator.compile(row).applyRarity(ordinal, context);

        // the ladder has ends, and a row that walks off one of them is a row to fix rather than a
        // reason to throw at whichever member happened to be holding the item
        return Rarity.findByOrdinal(Math.clamp((int) ordinal, 0, Rarity.size() - 1)).orElseThrow();
    }

    /**
     * Counts what a New Year Cake Bag is carrying, out of the gzipped inventory its tag holds.
     *
     * @param itemModel reference data for the item being read
     * @param compoundTag the item's NBT tag
     * @return the number of cakes stored, empty for anything that is not a readable cake bag
     */
    private static Optional<Integer> readNewYearCakes(@NotNull Item itemModel, @NotNull CompoundTag compoundTag) {
        if (!"NEW_YEAR_CAKE_BAG".equals(itemModel.getId()))
            return Optional.empty();

        try {
            byte[] nbtCakeBag = compoundTag.getPathOrDefault("tag.ExtraAttributes.new_year_cake_bag_data", ByteArrayTag.EMPTY).getValue();
            ListTag<CompoundTag> cakeBagItems = NbtFactory.fromByteArray(nbtCakeBag).getListTag("i");
            return Optional.of(cakeBagItems.size());
        } catch (NbtException ignore) {
            return Optional.empty();
        }
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
