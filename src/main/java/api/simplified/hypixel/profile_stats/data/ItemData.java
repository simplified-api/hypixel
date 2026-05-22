package api.simplified.hypixel.profile_stats.data;

import dev.sbs.skyblockdata.SkyBlockData;
import dev.sbs.skyblockdata.model.BonusItemStat;
import dev.sbs.skyblockdata.model.BonusReforgeStat;
import dev.sbs.skyblockdata.model.Enchantment;
import dev.sbs.skyblockdata.model.HotPotatoStat;
import dev.sbs.skyblockdata.model.Item;
import dev.sbs.skyblockdata.model.Stat;
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

public class ItemData extends ObjectData<ItemData.Type> {

    @Getter private final ConcurrentMap<Enchantment, Integer> enchantments;
    @Getter private final ConcurrentMap<Enchantment, ConcurrentList<Stat.Substitute>> enchantmentStats;
    @Getter private final Optional<BonusReforgeStat> bonusReforgeStatModel;
    @Getter private final int hotPotatoBooks;
    private final boolean hasArtOfWar;
    private final boolean hasArtOfPeace;
    @Getter private boolean bonusCalculated;

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

    public final boolean hasArtOfPeace() {
        return this.hasArtOfPeace;
    }

    public final boolean hasArtOfWar() {
        return this.hasArtOfWar;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(this.getEnchantments(), this.getEnchantmentStats(), this.getBonusReforgeStatModel(), this.getHotPotatoBooks(), this.hasArtOfWar(), this.isBonusCalculated());
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum Type implements ObjectData.Type {

        SUN_TZU(true),
        ENCHANTS(true),
        GEMSTONES(true),
        HOT_POTATOES(true),
        REFORGES(false),
        STATS(true);

        private final boolean optimizerConstant;

    }

}
