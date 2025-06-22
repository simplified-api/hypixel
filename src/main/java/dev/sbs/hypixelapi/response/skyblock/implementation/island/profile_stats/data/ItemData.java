package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.profile_stats.data;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.data.model.bonus_data.bonus_item_stats.BonusItemStatModel;
import dev.sbs.minecraftapi.data.model.bonus_data.bonus_reforge_stats.BonusReforgeStatModel;
import dev.sbs.minecraftapi.data.model.enchantment_data.enchantment_stats.EnchantmentStatModel;
import dev.sbs.minecraftapi.data.model.enchantment_data.enchantments.EnchantmentModel;
import dev.sbs.minecraftapi.data.model.hot_potato_stats.HotPotatoStatModel;
import dev.sbs.minecraftapi.data.model.items.ItemModel;
import dev.sbs.minecraftapi.data.model.reforge_data.reforge_stats.ReforgeStatModel;
import dev.sbs.minecraftapi.data.model.stats.StatModel;
import dev.sbs.api.stream.pair.Pair;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.IntTag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Optional;

public class ItemData extends ObjectData<ItemData.Type> {

    @Getter private final ConcurrentMap<EnchantmentModel, Integer> enchantments;
    @Getter private final ConcurrentMap<EnchantmentModel, ConcurrentList<EnchantmentStatModel>> enchantmentStats;
    @Getter private final Optional<BonusReforgeStatModel> bonusReforgeStatModel;
    @Getter private final Optional<ReforgeStatModel> reforgeStat;
    @Getter private final int hotPotatoBooks;
    private final boolean hasArtOfWar;
    private final boolean hasArtOfPeace;
    @Getter private boolean bonusCalculated;

    public ItemData(ItemModel itemModel, CompoundTag compoundTag) {
        super(itemModel, compoundTag);
        this.hotPotatoBooks = compoundTag.getPathOrDefault("tag.ExtraAttributes.hot_potato_count", IntTag.EMPTY).getValue();
        this.hasArtOfWar = compoundTag.containsPath("tag.ExtraAttributes.art_of_war_count");
        this.hasArtOfPeace = compoundTag.containsPath("tag.ExtraAttributes.artOfPeaceApplied");

        // Load Bonus Reforge Model
        this.bonusReforgeStatModel = this.getReforge().flatMap(reforgeModel -> SimplifiedApi.getRepositoryOf(BonusReforgeStatModel.class)
            .findFirst(BonusReforgeStatModel::getReforge, reforgeModel)
        );

        // Load Reforge Stat Model
        this.reforgeStat = this.getReforge().flatMap(reforgeModel -> SimplifiedApi.getRepositoryOf(ReforgeStatModel.class)
            .findFirst(
                Pair.of(ReforgeStatModel::getReforge, reforgeModel),
                Pair.of(ReforgeStatModel::getRarity, this.getRarity())
            )
        );

        // Save Stats
        itemModel.getStats().forEach((key, value) -> SimplifiedApi.getRepositoryOf(StatModel.class)
            .findFirst(StatModel::getKey, key)
            .ifPresent(statModel -> this.getStats(ItemData.Type.STATS).get(statModel).addBonus(value)));

        // Save Reforge Stats
        PlayerDataHelper.handleReforgeBonus(this.getReforgeStat())
            .forEach((statModel, value) -> this.getStats(ItemData.Type.REFORGES).get(statModel).addBonus(value));

        // Save Gemstone Stats
        PlayerDataHelper.handleGemstoneBonus(this)
            .forEach((statModel, value) -> this.getStats(ItemData.Type.GEMSTONES).get(statModel).addBonus(value));

        // Save Hot Potato Book Stats
        SimplifiedApi.getRepositoryOf(HotPotatoStatModel.class)
            .matchAll(hotPotatoStatModel -> hotPotatoStatModel.getItemTypes().contains(itemModel.getType().getKey()))
            .forEach(hotPotatoStatModel -> this.getStats(ItemData.Type.HOT_POTATOES).get(hotPotatoStatModel.getStat()).addBonus(this.getHotPotatoBooks() * hotPotatoStatModel.getValue()));

        // Save Art Of Peace Stats
        if (this.hasArtOfPeace()) {
            SimplifiedApi.getRepositoryOf(StatModel.class)
                .findFirst(StatModel::getKey, "HEALTH")
                .ifPresent(statModel -> this.getStats(Type.SUN_TZU).get(statModel).addBonus(40.0));
        }

        // Save Art Of War Stats
        if (this.hasArtOfWar()) {
            SimplifiedApi.getRepositoryOf(StatModel.class)
                .findFirst(StatModel::getKey, "STRENGTH")
                .ifPresent(statModel -> this.getStats(Type.SUN_TZU).get(statModel).addBonus(5.0));
        }

        // Save Enchantment Stats
        ConcurrentMap<EnchantmentModel, Integer> enchantments = Concurrent.newMap();
        ConcurrentMap<EnchantmentModel, ConcurrentList<EnchantmentStatModel>> enchantmentStats = Concurrent.newMap();

        compoundTag.getPathOrDefault("tag.ExtraAttributes.enchantments", CompoundTag.EMPTY)
            .entrySet()
            .stream()
            .map(entry -> Pair.of(
                SimplifiedApi.getRepositoryOf(EnchantmentModel.class)
                    .findFirstOrNull(EnchantmentModel::getKey, entry.getKey().toUpperCase()),
                ((IntTag)entry.getValue()).getValue()
            ))
            .filter(enchantmentData -> Objects.nonNull(enchantmentData.getLeft()))
            .forEach(enchantmentData -> {
                enchantments.put(enchantmentData.getKey(), enchantmentData.getValue());
                enchantmentStats.put(enchantmentData.getKey(), Concurrent.newList());

                // Handle Enchantment Stat Models
                SimplifiedApi.getRepositoryOf(EnchantmentStatModel.class)
                    .findAll(EnchantmentStatModel::getEnchantment, enchantmentData.getLeft())
                    .filter(enchantmentStatModel -> enchantmentStatModel.getLevels().stream().anyMatch(level -> enchantmentData.getValue() >= level)) // Contains Level
                    .forEach(enchantmentStatModel -> enchantmentStats.get(enchantmentData.getKey()).add(enchantmentStatModel));

                // Handle Enchantment Stats
                if (enchantmentData.getLeft().getMobTypes().isEmpty()) {
                    enchantmentStats.get(enchantmentData.getKey())
                        .stream()
                        .filter(EnchantmentStatModel::notPercentage) // Static Only
                        .filter(EnchantmentStatModel::hasStat) // Has Stat
                        .forEach(enchantmentStatModel -> {
                            double enchantBonus = enchantmentStatModel.getLevels().stream().filter(level -> enchantmentData.getValue() >= level).mapToDouble(__ -> enchantmentStatModel.getLevelBonus()).sum();
                            this.getStats(Type.ENCHANTS).get(enchantmentStatModel.getStat()).addBonus(enchantBonus);
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
            this.getBonusReforgeStatModel().ifPresent(bonusReforgeStatModel -> this.getStats(ItemData.Type.REFORGES)
                .forEach((statModel, statData) -> statData.bonus = PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), this.getCompoundTag(), expressionVariables, bonusReforgeStatModel))
            );

            // Handle Bonus Item Stats
            this.getBonusItemStatModels()
                .stream()
                .filter(BonusItemStatModel::noRequiredMobType)
                .forEach(bonusItemStatModel -> {
                    // Handle Bonus Gemstone Stats
                    if (bonusItemStatModel.isForGems()) {
                        this.getStats(Type.GEMSTONES)
                            .forEach((statModel, statData) -> statData.bonus = PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), this.getCompoundTag(), expressionVariables, bonusItemStatModel));
                    }

                    // Handle Bonus Reforges
                    if (bonusItemStatModel.isForReforges()) {
                        this.getStats(Type.REFORGES)
                            .forEach((statModel, statData) -> statData.bonus = PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), this.getCompoundTag(), expressionVariables, bonusItemStatModel));
                    }

                    // Handle Bonus Stats
                    if (bonusItemStatModel.isForStats()) {
                        this.getStats(Type.STATS)
                            .forEach((statModel, statData) -> statData.bonus = PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), this.getCompoundTag(), expressionVariables, bonusItemStatModel));
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

        return new EqualsBuilder()
            .append(this.getHotPotatoBooks(), itemData.getHotPotatoBooks())
            .append(this.isBonusCalculated(), itemData.isBonusCalculated())
            .append(this.getEnchantments(), itemData.getEnchantments())
            .append(this.getEnchantmentStats(), itemData.getEnchantmentStats())
            .append(this.getBonusReforgeStatModel(), itemData.getBonusReforgeStatModel())
            .append(this.getReforgeStat(), itemData.getReforgeStat())
            .append(this.hasArtOfWar(), itemData.hasArtOfWar())
            .build();
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
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(this.getEnchantments())
            .append(this.getEnchantmentStats())
            .append(this.getBonusReforgeStatModel())
            .append(this.getReforgeStat())
            .append(this.getHotPotatoBooks())
            .append(this.hasArtOfWar())
            .append(this.isBonusCalculated())
            .build();
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