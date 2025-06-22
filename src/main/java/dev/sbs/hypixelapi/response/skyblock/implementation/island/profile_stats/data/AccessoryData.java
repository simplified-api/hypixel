package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.profile_stats.data;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.search.SearchFunction;
import dev.sbs.minecraftapi.data.model.accessory_data.accessories.AccessoryModel;
import dev.sbs.minecraftapi.data.model.accessory_data.accessory_enrichments.AccessoryEnrichmentModel;
import dev.sbs.minecraftapi.data.model.bonus_data.bonus_item_stats.BonusItemStatModel;
import dev.sbs.minecraftapi.data.model.rarities.RarityModel;
import dev.sbs.minecraftapi.data.model.stats.StatModel;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.nbt.exception.NbtException;
import dev.sbs.minecraftapi.nbt.tags.array.ByteArrayTag;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.collection.ListTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.IntTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Getter
public class AccessoryData extends ObjectData<AccessoryData.Type> {

    private static final ConcurrentList<Integer> PULSE_CHARGES = Concurrent.newList(150_000, 1_000_000, 5_000_000);
    private final AccessoryModel accessory;
    private boolean bonusCalculated;
    private final Optional<AccessoryEnrichmentModel> enrichment;

    public AccessoryData(AccessoryModel accessory, CompoundTag compoundTag) {
        super(accessory.getItem(), compoundTag);
        this.accessory = accessory;

        // Load Enrichment
        this.enrichment = SimplifiedApi.getRepositoryOf(AccessoryEnrichmentModel.class).findFirst(
            SearchFunction.combine(AccessoryEnrichmentModel::getStat, StatModel::getKey),
            compoundTag.getPathOrDefault("tag.ExtraAttributes.talisman_enrichment", StringTag.EMPTY).getValue().toUpperCase()
        );

        // Handle Gemstone Stats
        PlayerDataHelper.handleGemstoneBonus(this)
            .forEach((statModel, value) -> this.addBonus(this.getStats(AccessoryData.Type.GEMSTONES).get(statModel), value));

        // Handle Stats
        this.getAccessory().getEffects().forEach((key, value) -> SimplifiedApi.getRepositoryOf(StatModel.class).findFirst(StatModel::getKey, key)
            .ifPresent(statModel -> this.addBonus(this.getStats(AccessoryData.Type.STATS).get(statModel), value)));

        // Handle Enrichment Stats
        this.getEnrichment()
            .ifPresent(accessoryEnrichmentModel -> this.addBonus(this.getStats(AccessoryData.Type.ENRICHMENTS).get(accessoryEnrichmentModel.getStat()), accessoryEnrichmentModel.getValue()));

        // New Year Cake Bag
        if ("NEW_YEAR_CAKE_BAG".equals(this.getAccessory().getItem().getItemId())) {
            try {
                Byte[] nbtCakeBag = compoundTag.getPathOrDefault("tag.ExtraAttributes.new_year_cake_bag_data", ByteArrayTag.EMPTY).getValue();
                ListTag<CompoundTag> cakeBagItems = MinecraftApi.getNbtFactory().fromByteArray(nbtCakeBag).getListTag("i");
                SimplifiedApi.getRepositoryOf(StatModel.class).findFirst(StatModel::getKey, "HEALTH")
                    .ifPresent(statModel -> this.addBonus(this.getStats(AccessoryData.Type.CAKE_BAG).get(statModel), cakeBagItems.size()));
            } catch (NbtException ignore) { }
        }
    }

    @Override
    protected int handleRarityUpgrades(int rarityOrdinal) {
        int increaseRarity = 0;

        if (this.getItem().getItemId().equals("POWER_ARTIFACT")) {
            long perfects = this.getGemstones()
                .stream()
                .flatMap(entry -> entry.getValue().stream())
                .filter(gemstoneTypeModel -> gemstoneTypeModel.getKey().equals("PERFECT"))
                .count();

            increaseRarity = (perfects == 7) ? 1 : 0;
        }

        if (this.getItem().getItemId().equals("PANDORAS_BOX")) {
            increaseRarity = SimplifiedApi.getRepositoryOf(RarityModel.class)
                .findFirst(RarityModel::getKey, super.getCompoundTag().getPathOrDefault("tag.ExtraAttributes.pandora-rarity", StringTag.EMPTY).getValue())
                .map(RarityModel::getOrdinal)
                .orElse(rarityOrdinal) - rarityOrdinal;
        }

        if (this.getItem().getItemId().equals("PULSE_RING")) {
            int thunderCharge = this.getCompoundTag().getPathOrDefault("tag.ExtraAttributes.thunder_charge", IntTag.EMPTY).getValue();

            for (int i = 0; i < PULSE_CHARGES.size(); i++) {
                if (thunderCharge >= PULSE_CHARGES.get(i))
                    increaseRarity++;
            }
        }

        if (this.getItem().getItemId().equals("TRAPPER_CREST")) {
            int pelts = this.getCompoundTag().getPathOrDefault("tag.ExtraAttributes.pelts_earned", IntTag.EMPTY).getValue();
            increaseRarity = (pelts >= 500) ? 1 : 0;
        }

        return rarityOrdinal + increaseRarity;
    }

    @Override
    public AccessoryData calculateBonus(ConcurrentMap<String, Double> expressionVariables) {
        if (!this.isBonusCalculated()) {
            this.bonusCalculated = true;

            // Handle Bonus Item Stats
            this.getBonusItemStatModels()
                .stream()
                .filter(BonusItemStatModel::noRequiredMobType)
                .forEach(bonusItemStatModel -> {
                    // Handle Bonus Gemstone Stats
                    if (bonusItemStatModel.isForGems()) {
                        this.getStats(AccessoryData.Type.GEMSTONES)
                            .forEach((statModel, statData) -> statData.bonus = PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), this.getCompoundTag(), expressionVariables, bonusItemStatModel));
                    }

                    // Handle Bonus Stats
                    if (bonusItemStatModel.isForStats()) {
                        this.getStats(AccessoryData.Type.STATS)
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

        AccessoryData that = (AccessoryData) o;

        return new EqualsBuilder()
            .append(this.isBonusCalculated(), that.isBonusCalculated())
            .append(this.getAccessory(), that.getAccessory())
            .build();
    }

    @Override
    protected Type[] getAllTypes() {
        return AccessoryData.Type.values();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(this.getAccessory())
            .append(this.isBonusCalculated())
            .build();
    }

    public final boolean isMissingEnrichment() {
        return this.getRarity().isEnrichable() && this.getEnrichment().isEmpty();
    }

    @Getter
    @RequiredArgsConstructor
    public enum Type implements ObjectData.Type {

        CAKE_BAG(true),
        GEMSTONES(true),
        STATS(true),
        ENRICHMENTS(true);

        private final boolean optimizerConstant;

    }

}
