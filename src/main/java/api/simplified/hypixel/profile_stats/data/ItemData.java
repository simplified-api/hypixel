package api.simplified.hypixel.profile_stats.data;

import api.simplified.hypixel.profile_stats.ReferenceSnapshot;
import api.simplified.hypixel.profile_stats.StatOrigin;
import api.simplified.skyblock.model.BonusItemStat;
import api.simplified.skyblock.model.BonusReforgeStat;
import api.simplified.skyblock.model.BuffEffectsModel;
import api.simplified.skyblock.model.Enchantment;
import api.simplified.skyblock.model.Item;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.IntTag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One equipped or held item, with every stat its modifiers contribute already totalled.
 * <p>
 * An item stacks more sources than an accessory does - enchantments, a reforge, gemstones, hot potato
 * books and the two Sun Tzu scrolls all add on top of what the item itself gives, and each is kept in
 * its own bucket so a caller can see where a number came from. An enchantment that only applies to
 * certain mobs is recorded but not totalled, since whether it counts depends on what is being fought.
 */
public class ItemData extends ObjectData<ItemData.Type> {

    /**
     * Level of each enchantment on the item, keyed by the enchantment.
     */
    @Getter private final ConcurrentMap<Enchantment, Integer> enchantments;

    /**
     * Stat substitutes each enchantment qualifies for at its level, including the mob-specific ones
     * left out of the totals.
     */
    @Getter private final ConcurrentMap<Enchantment, ConcurrentList<Stat.Substitute>> enchantmentStats;

    /**
     * Conditional bonus declared for the applied reforge, empty when it has none or none is applied.
     */
    @Getter private final Optional<BonusReforgeStat> bonusReforgeStatModel;

    /**
     * Hot potato books applied, counting both hot potato books and fuming potato books.
     */
    @Getter private final int hotPotatoBooks;

    private final boolean hasArtOfWar;
    private final boolean hasArtOfPeace;

    /**
     * Whether the conditional bonuses have already been evaluated.
     */
    @Getter private boolean bonusCalculated;

    /**
     * Constructs a new {@code ItemData} and totals every unconditional modifier on the item.
     *
     * @param reference the reference tables to resolve against
     * @param itemModel reference data for the item being read
     * @param compoundTag the item's NBT tag
     */
    public ItemData(@NotNull ReferenceSnapshot reference, Item itemModel, CompoundTag compoundTag) {
        super(reference, itemModel, compoundTag);
        this.hotPotatoBooks = compoundTag.getPathOrDefault("tag.ExtraAttributes.hot_potato_count", IntTag.EMPTY).getValue();
        this.hasArtOfWar = compoundTag.containsPath("tag.ExtraAttributes.art_of_war_count");
        this.hasArtOfPeace = compoundTag.containsPath("tag.ExtraAttributes.artOfPeaceApplied");

        // Load Bonus Reforge Model
        this.bonusReforgeStatModel = this.getReforge().flatMap(reforge -> reference.getBonusReforgeStat(reforge.getId()));

        // Save Stats
        itemModel.getStats().forEach((key, value) -> this.table.add(Type.STATS, key, StatHalf.BONUS, value));

        // Save Reforge Stats
        PlayerDataHelper.handleReforgeBonus(this.getReforge(), this.getRarity())
            .forEach((statModel, value) -> this.table.add(Type.REFORGES, statModel, StatHalf.BONUS, value));

        // Save Gemstone Stats
        PlayerDataHelper.handleGemstoneBonus(this)
            .forEach((statModel, value) -> this.table.add(Type.GEMSTONES, statModel, StatHalf.BONUS, value));

        // Save Hot Potato Book Stats
        reference.getHotPotatoStats(itemModel.getCategory().getId())
            .forEach(hotPotatoStat -> this.table.add(Type.HOT_POTATOES, hotPotatoStat.getStat(), StatHalf.BONUS, this.getHotPotatoBooks() * hotPotatoStat.getValue()));

        // Save Art Of Peace Stats
        if (this.hasArtOfPeace())
            this.table.add(Type.SUN_TZU, "HEALTH", StatHalf.BONUS, 40.0);

        // Save Art Of War Stats
        if (this.hasArtOfWar())
            this.table.add(Type.SUN_TZU, "STRENGTH", StatHalf.BONUS, 5.0);

        // Save Enchantment Stats
        ConcurrentMap<Enchantment, Integer> enchantments = Concurrent.newMap();
        ConcurrentMap<Enchantment, ConcurrentList<Stat.Substitute>> enchantmentStats = Concurrent.newMap();

        compoundTag.getPathOrDefault("tag.ExtraAttributes.enchantments", CompoundTag.EMPTY)
            .entrySet()
            .stream()
            .map(entry -> Pair.of(
                reference.getEnchantment(entry.getKey().toUpperCase()).orElse(null),
                ((IntTag)entry.getValue()).getValue()
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
                            this.table.add(Type.ENCHANTS, sub.getStat().get(), StatHalf.BONUS, enchantBonus);
                        });
                }
            });

        this.enchantments = enchantments;
        this.enchantmentStats = enchantmentStats;
    }

    @Override
    public ItemData calculateBonus(ConcurrentMap<String, Double> expressionVariables) {
        if (!this.isBonusCalculated()) {
            this.bonusCalculated = true;

            // Handle Reforges
            this.getBonusReforgeStatModel().ifPresent(bonusReforgeStat -> this.applyBonus(Type.REFORGES, expressionVariables, bonusReforgeStat));

            // Handle Bonus Item Stats
            this.getBonusItemStatModels()
                .stream()
                .filter(BonusItemStat::noRequiredMobType)
                .forEach(bonusItemStat -> {
                    // Handle Bonus Gemstone Stats
                    if (bonusItemStat.isForGems())
                        this.applyBonus(Type.GEMSTONES, expressionVariables, bonusItemStat);

                    // Handle Bonus Reforges
                    if (bonusItemStat.isForReforges())
                        this.applyBonus(Type.REFORGES, expressionVariables, bonusItemStat);

                    // Handle Bonus Stats
                    if (bonusItemStat.isForStats())
                        this.applyBonus(Type.STATS, expressionVariables, bonusItemStat);
                });
        }

        return this;
    }

    private void applyBonus(@NotNull Type bucket, ConcurrentMap<String, Double> expressionVariables, @NotNull BuffEffectsModel bonusModel) {
        this.getStats(bucket).forEach((statModel, statData) -> StatHalf.BONUS.set(
            statData,
            PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), this.getCompoundTag(), expressionVariables, bonusModel)
        ));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ItemData itemData = (ItemData) o;

        return this.getHotPotatoBooks() == itemData.getHotPotatoBooks()
            && this.isBonusCalculated() == itemData.isBonusCalculated()
            && Objects.equals(this.getEnchantments(), itemData.getEnchantments())
            && Objects.equals(this.getEnchantmentStats(), itemData.getEnchantmentStats())
            && Objects.equals(this.getBonusReforgeStatModel(), itemData.getBonusReforgeStatModel())
            && this.hasArtOfWar() == itemData.hasArtOfWar();
    }

    @Override
    protected Type[] getAllTypes() {
        return ItemData.Type.values();
    }

    /**
     * Whether The Art of Peace has been applied, adding a flat forty health.
     */
    public final boolean hasArtOfPeace() {
        return this.hasArtOfPeace;
    }

    /**
     * Whether The Art of War has been applied, adding a flat five strength.
     */
    public final boolean hasArtOfWar() {
        return this.hasArtOfWar;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(this.getEnchantments(), this.getEnchantmentStats(), this.getBonusReforgeStatModel(), this.getHotPotatoBooks(), this.hasArtOfWar(), this.isBonusCalculated());
    }

    /**
     * The six sources an item's stats are split between.
     * <p>
     * Every source but the reforge is fixed for the item, so an optimiser totals them once. A reforge
     * is the one thing it is free to change, which is why it has to be recomputed per candidate.
     */
    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum Type implements StatOrigin {

        /**
         * The flat bonuses from The Art of War and The Art of Peace.
         */
        SUN_TZU(true),

        /**
         * Stats from enchantments, counting only those that apply against anything.
         */
        ENCHANTS(true),

        /**
         * Stats from the gemstones slotted into the item.
         */
        GEMSTONES(true),

        /**
         * Stats from hot potato books, scaled by how many the item carries.
         */
        HOT_POTATOES(true),

        /**
         * Stats from the applied reforge, scaled by the item's rarity.
         */
        REFORGES(false),

        /**
         * Stats the item provides in its own right.
         */
        STATS(true);

        /**
         * Whether this source is fixed for the item, so an optimiser need not recompute it.
         */
        private final boolean optimizerConstant;

    }

}
