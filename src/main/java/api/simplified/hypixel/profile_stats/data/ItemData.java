package api.simplified.hypixel.profile_stats.data;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.BonusItemStat;
import api.simplified.skyblock.model.BonusReforgeStat;
import api.simplified.skyblock.model.Enchantment;
import api.simplified.skyblock.model.HotPotatoStat;
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
     * @param itemModel reference data for the item being read
     * @param compoundTag the item's NBT tag
     */
    public ItemData(Item itemModel, CompoundTag compoundTag) {
        super(itemModel, compoundTag);
        this.hotPotatoBooks = compoundTag.getPathOrDefault("tag.ExtraAttributes.hot_potato_count", IntTag.EMPTY).getValue();
        this.hasArtOfWar = compoundTag.containsPath("tag.ExtraAttributes.art_of_war_count");
        this.hasArtOfPeace = compoundTag.containsPath("tag.ExtraAttributes.artOfPeaceApplied");

        // Load Bonus Reforge Model
        this.bonusReforgeStatModel = this.getReforge().flatMap(reforge -> SkyBlockData.getRepository(BonusReforgeStat.class)
            .findFirst(BonusReforgeStat::getReforgeId, reforge.getId())
        );

        // Save Stats
        itemModel.getStats().forEach((key, value) -> SkyBlockData.getRepository(Stat.class)
            .findFirst(Stat::getId, key)
            .ifPresent(statModel -> this.getStats(ItemData.Type.STATS).get(statModel).addBonus(value)));

        // Save Reforge Stats
        PlayerDataHelper.handleReforgeBonus(this.getReforge(), this.getRarity())
            .forEach((statModel, value) -> this.getStats(ItemData.Type.REFORGES).get(statModel).addBonus(value));

        // Save Gemstone Stats
        PlayerDataHelper.handleGemstoneBonus(this)
            .forEach((statModel, value) -> this.getStats(ItemData.Type.GEMSTONES).get(statModel).addBonus(value));

        // Save Hot Potato Book Stats
        SkyBlockData.getRepository(HotPotatoStat.class)
            .matchAll(hotPotatoStat -> hotPotatoStat.getItemTypes().contains(itemModel.getCategory().getId()))
            .forEach(hotPotatoStat -> this.getStats(ItemData.Type.HOT_POTATOES).get(hotPotatoStat.getStat()).addBonus(this.getHotPotatoBooks() * hotPotatoStat.getValue()));

        // Save Art Of Peace Stats
        if (this.hasArtOfPeace()) {
            SkyBlockData.getRepository(Stat.class)
                .findFirst(Stat::getId, "HEALTH")
                .ifPresent(statModel -> this.getStats(Type.SUN_TZU).get(statModel).addBonus(40.0));
        }

        // Save Art Of War Stats
        if (this.hasArtOfWar()) {
            SkyBlockData.getRepository(Stat.class)
                .findFirst(Stat::getId, "STRENGTH")
                .ifPresent(statModel -> this.getStats(Type.SUN_TZU).get(statModel).addBonus(5.0));
        }

        // Save Enchantment Stats
        ConcurrentMap<Enchantment, Integer> enchantments = Concurrent.newMap();
        ConcurrentMap<Enchantment, ConcurrentList<Stat.Substitute>> enchantmentStats = Concurrent.newMap();

        compoundTag.getPathOrDefault("tag.ExtraAttributes.enchantments", CompoundTag.EMPTY)
            .entrySet()
            .stream()
            .map(entry -> Pair.of(
                SkyBlockData.getRepository(Enchantment.class)
                    .findFirstOrNull(Enchantment::getId, entry.getKey().toUpperCase()),
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
                            this.getStats(Type.ENCHANTS).get(sub.getStat().get()).addBonus(enchantBonus);
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
            this.getBonusReforgeStatModel().ifPresent(bonusReforgeStat -> this.getStats(ItemData.Type.REFORGES)
                .forEach((statModel, statData) -> statData.bonus = PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), this.getCompoundTag(), expressionVariables, bonusReforgeStat))
            );

            // Handle Bonus Item Stats
            this.getBonusItemStatModels()
                .stream()
                .filter(BonusItemStat::noRequiredMobType)
                .forEach(bonusItemStat -> {
                    // Handle Bonus Gemstone Stats
                    if (bonusItemStat.isForGems()) {
                        this.getStats(Type.GEMSTONES)
                            .forEach((statModel, statData) -> statData.bonus = PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), this.getCompoundTag(), expressionVariables, bonusItemStat));
                    }

                    // Handle Bonus Reforges
                    if (bonusItemStat.isForReforges()) {
                        this.getStats(Type.REFORGES)
                            .forEach((statModel, statData) -> statData.bonus = PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), this.getCompoundTag(), expressionVariables, bonusItemStat));
                    }

                    // Handle Bonus Stats
                    if (bonusItemStat.isForStats()) {
                        this.getStats(Type.STATS)
                            .forEach((statModel, statData) -> statData.bonus = PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), this.getCompoundTag(), expressionVariables, bonusItemStat));
                    }
                });
        }

        return this;
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
    public enum Type implements ObjectData.Type {

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
