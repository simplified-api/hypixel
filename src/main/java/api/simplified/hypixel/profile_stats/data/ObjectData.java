package api.simplified.hypixel.profile_stats.data;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.model.BonusItemStat;
import api.simplified.skyblock.model.Gemstone;
import api.simplified.skyblock.model.Item;
import api.simplified.skyblock.model.Reforge;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.util.StringUtil;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.IntTag;
import lib.minecraft.nbt.tag.StringTag;
import lib.minecraft.nbt.tag.Tag;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One item instance, read out of its NBT tag and paired with the reference data it names.
 * <p>
 * An item on the wire is a tag tree, not a stat sheet - the modifiers that decide what it actually
 * gives sit under {@code tag.ExtraAttributes} as ids, and mean nothing until each is looked up. The
 * constructor does that lookup once, so every subclass starts from a resolved item and only has to
 * add whatever is particular to its own kind. All of it needs a session.
 *
 * @param <T> the bucket type this item splits its stats by
 */
public abstract class ObjectData<T extends ObjectData.Type> extends StatData<T> {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("M/d/yy h:m a", Locale.US);
    private static final ZoneId HYPIXEL_TIMEZONE = ZoneId.of("America/New_York");

    /**
     * Reference data for the item this instance is of.
     */
    @Getter private final Item item;

    /**
     * The item's NBT tag exactly as it was decoded, still holding everything not read out here.
     */
    @Getter private final CompoundTag compoundTag;

    /**
     * Rarity after every upgrade the instance carries, which may sit above the item's own.
     */
    @Getter private final Rarity rarity;

    /**
     * Conditional stat bonuses declared for this item, still to be evaluated.
     */
    @Getter private final ConcurrentList<BonusItemStat> bonusItemStatModels;

    /**
     * Reforge applied to the instance, empty when it carries none.
     */
    @Getter private final Optional<Reforge> reforge;

    /**
     * Quality of each gemstone slotted into the instance, keyed by the gemstone.
     */
    @Getter private final ConcurrentMap<Gemstone, ConcurrentList<Gemstone.Type>> gemstones;

    /**
     * Whether a recombobulator has raised the instance one rarity.
     */
    @Getter private final boolean recombobulated;

    /**
     * Whether a tier boost has raised the instance one rarity.
     */
    @Getter private final boolean tierBoosted;

    /**
     * When the instance was obtained, empty for anything old enough to predate the stamp.
     */
    @Getter private final Optional<Long> timestamp;

    /**
     * Constructs a new {@code ObjectData} by resolving an item's tag against the repositories.
     *
     * @param itemModel reference data for the item being read
     * @param compoundTag the item's NBT tag
     */
    protected ObjectData(Item itemModel, CompoundTag compoundTag) {
        this.item = itemModel;
        this.compoundTag = compoundTag;

        // Load Timestamp
        this.timestamp = Optional.ofNullable(
                StringUtil.defaultIfEmpty(
                    compoundTag.getPathOrDefault("tag.ExtraAttributes.timestamp", StringTag.EMPTY).getValue(),
                    null
                )
            )
            .map(timestamp -> LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT))
            .map(localDateTime -> localDateTime.atZone(HYPIXEL_TIMEZONE))
            .map(ZonedDateTime::toInstant)
            .map(Instant::toEpochMilli);

        // Load Recombobulator
        this.recombobulated = compoundTag.getPathOrDefault("tag.ExtraAttributes.rarity_upgrades", IntTag.EMPTY).getValue() == 1;

        // Load Tier Boost
        this.tierBoosted = compoundTag.getPathOrDefault("tag.ExtraAttributes.baseStatBoostPercentage", IntTag.EMPTY).getValue() > 0;

        // Load Gemstones
        CompoundTag gemTag = compoundTag.getPathOrDefault("tag.ExtraAttributes.gems", CompoundTag.EMPTY);
        this.gemstones = Concurrent.newUnmodifiableMap(gemTag.notEmpty() ? findGemstones(gemTag) : Concurrent.newMap());

        // Initialize Stats
        ConcurrentList<Stat> statModels = SkyBlockData.getRepository(Stat.class).findAll();
        Arrays.stream(this.getAllTypes()).forEach(type -> {
            this.stats.put(type, Concurrent.newLinkedMap());
            statModels.forEach(statModel -> this.stats.get(type).put(statModel, new Data()));
        });

        // Load Bonus Item Stat Model
        this.bonusItemStatModels = SkyBlockData.getRepository(BonusItemStat.class)
            .findAll(BonusItemStat::getItemId, itemModel.getId())
            .collect(Concurrent.toUnmodifiableList());

        // Load Reforge Model
        this.reforge = SkyBlockData.getRepository(Reforge.class)
            .findFirst(Reforge::getId, this.getCompoundTag()
                .getPathOrDefault("tag.ExtraAttributes.modifier", StringTag.EMPTY)
                .getValue()
                .toUpperCase()
            );

        // Load Rarity
        this.rarity = Rarity.of(this.handleRarityUpgrades(
            itemModel.getRarity().ordinal() +
                (this.isRecombobulated() ? 1 : 0) +
                (this.isTierBoosted() ? 1 : 0)
        ));
    }

    /**
     * Evaluates the conditional bonuses this item declares and folds them into its buckets.
     * <p>
     * The bonuses are expressions over the player's own state, so they cannot be resolved when the
     * item is read - the variables only exist once the whole profile is known. Calling this twice
     * does nothing the second time.
     *
     * @param expressionVariables the player state the bonus expressions are evaluated against
     * @return this item
     */
    public abstract ObjectData<T> calculateBonus(ConcurrentMap<String, Double> expressionVariables);

    private static ConcurrentMap<Gemstone, ConcurrentList<Gemstone.Type>> findGemstones(CompoundTag gemTag) {
        ConcurrentList<Gemstone> gemstoneModels = SkyBlockData.getRepository(Gemstone.class).findAll();
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

    /**
     * Adjusts the rarity for upgrades particular to one item, applied on top of the generic ones.
     *
     * @param rarityOrdinal the rarity already raised by a recombobulator and a tier boost
     * @return the adjusted ordinal, unchanged unless a subclass overrides this
     */
    protected int handleRarityUpgrades(int rarityOrdinal) {
        return rarityOrdinal;
    }

    /**
     * Whether the conditional bonuses have already been evaluated.
     */
    public abstract boolean isBonusCalculated();

    /**
     * Whether no recombobulator has been applied.
     */
    public final boolean notRecombobulated() {
        return !this.isRecombobulated();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectData<?> that = (ObjectData<?>) o;

        return this.isRecombobulated() == that.isRecombobulated()
            && this.isTierBoosted() == that.isTierBoosted()
            && Objects.equals(this.getItem(), that.getItem())
            && Objects.equals(this.getCompoundTag(), that.getCompoundTag())
            && Objects.equals(this.getRarity(), that.getRarity())
            && Objects.equals(this.getBonusItemStatModels(), that.getBonusItemStatModels())
            && Objects.equals(this.getReforge(), that.getReforge())
            && Objects.equals(this.getGemstones(), that.getGemstones())
            && Objects.equals(this.getTimestamp(), that.getTimestamp());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getItem(), this.getCompoundTag(), this.getRarity(), this.getBonusItemStatModels(), this.getReforge(), this.getGemstones(), this.isRecombobulated(), this.isTierBoosted(), this.getTimestamp());
    }

    /**
     * A bucket an item's stats can be split into.
     */
    public interface Type {

        /**
         * Whether this bucket's contribution is fixed for the item, so an optimiser can treat it as
         * a constant rather than recomputing it per candidate.
         */
        boolean isOptimizerConstant();

    }

}
