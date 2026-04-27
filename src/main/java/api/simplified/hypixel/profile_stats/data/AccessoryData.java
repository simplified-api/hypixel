package api.simplified.hypixel.profile_stats.data;

import dev.sbs.skyblockdata.SkyBlockData;
import dev.sbs.skyblockdata.common.Rarity;
import dev.sbs.skyblockdata.model.Accessory;
import dev.sbs.skyblockdata.model.BonusItemStat;
import dev.sbs.skyblockdata.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lib.minecraft.nbt.NbtFactory;
import lib.minecraft.nbt.exception.NbtException;
import lib.minecraft.nbt.tags.array.ByteArrayTag;
import lib.minecraft.nbt.tags.collection.CompoundTag;
import lib.minecraft.nbt.tags.collection.ListTag;
import lib.minecraft.nbt.tags.primitive.IntTag;
import lib.minecraft.nbt.tags.primitive.StringTag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Optional;

@Getter
public class AccessoryData extends ObjectData<AccessoryData.Type> {

    private static final ConcurrentList<Integer> PULSE_CHARGES = Concurrent.newList(150_000, 1_000_000, 5_000_000);
    private final Accessory accessory;
    private boolean bonusCalculated;
    private final Optional<Stat> enrichmentStat;

    public AccessoryData(Accessory accessory, CompoundTag compoundTag) {
        super(accessory.getItem(), compoundTag);
        this.accessory = accessory;

        // Load Enrichment - look up stat by enrichment key from NBT
        String enrichmentKey = compoundTag.getPathOrDefault("tag.ExtraAttributes.talisman_enrichment", StringTag.EMPTY).getValue().toUpperCase();
        this.enrichmentStat = SkyBlockData.getRepository(Stat.class)
            .findFirst(Stat::getId, enrichmentKey);

        // Handle Gemstone Stats
        PlayerDataHelper.handleGemstoneBonus(this)
            .forEach((statModel, value) -> this.addBonus(this.getStats(AccessoryData.Type.GEMSTONES).get(statModel), value));

        // Handle Stats
        this.getAccessory().getItem().getStats().forEach((key, value) -> SkyBlockData.getRepository(Stat.class).findFirst(Stat::getId, key)
            .ifPresent(statModel -> this.addBonus(this.getStats(AccessoryData.Type.STATS).get(statModel), value)));

        // Handle Enrichment Stats
        this.getEnrichmentStat()
            .filter(stat -> stat.getEnrichment() > 0.0)
            .ifPresent(stat -> this.addBonus(this.getStats(AccessoryData.Type.ENRICHMENTS).get(stat), stat.getEnrichment()));

        // New Year Cake Bag
        if ("NEW_YEAR_CAKE_BAG".equals(this.getAccessory().getItem().getId())) {
            try {
                byte[] nbtCakeBag = compoundTag.getPathOrDefault("tag.ExtraAttributes.new_year_cake_bag_data", ByteArrayTag.EMPTY).getValue();
                ListTag<CompoundTag> cakeBagItems = NbtFactory.fromByteArray(nbtCakeBag).getListTag("i");
                SkyBlockData.getRepository(Stat.class).findFirst(Stat::getId, "HEALTH")
                    .ifPresent(statModel -> this.addBonus(this.getStats(AccessoryData.Type.CAKE_BAG).get(statModel), cakeBagItems.size()));
            } catch (NbtException ignore) { }
        }
    }

    @Override
    protected int handleRarityUpgrades(int rarityOrdinal) {
        int increaseRarity = 0;

        if (this.getItem().getId().equals("POWER_ARTIFACT")) {
            long perfects = this.getGemstones()
                .stream()
                .flatMap(entry -> entry.getValue().stream())
                .filter(gemstoneType -> gemstoneType.name().equals("PERFECT"))
                .count();

            increaseRarity = (perfects == 7) ? 1 : 0;
        }

        if (this.getItem().getId().equals("PANDORAS_BOX")) {
            String pandoraRarityKey = super.getCompoundTag().getPathOrDefault("tag.ExtraAttributes.pandora-rarity", StringTag.EMPTY).getValue().toUpperCase();
            try {
                Rarity pandoraRarity = Rarity.valueOf(pandoraRarityKey);
                increaseRarity = pandoraRarity.ordinal() - rarityOrdinal;
            } catch (IllegalArgumentException ignore) { }
        }

        if (this.getItem().getId().equals("PULSE_RING")) {
            int thunderCharge = this.getCompoundTag().getPathOrDefault("tag.ExtraAttributes.thunder_charge", IntTag.EMPTY).getValue();

            for (int i = 0; i < PULSE_CHARGES.size(); i++) {
                if (thunderCharge >= PULSE_CHARGES.get(i))
                    increaseRarity++;
            }
        }

        if (this.getItem().getId().equals("TRAPPER_CREST")) {
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
                .filter(BonusItemStat::noRequiredMobType)
                .forEach(bonusItemStat -> {
                    // Handle Bonus Gemstone Stats
                    if (bonusItemStat.isForGems()) {
                        this.getStats(AccessoryData.Type.GEMSTONES)
                            .forEach((statModel, statData) -> statData.bonus = PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), this.getCompoundTag(), expressionVariables, bonusItemStat));
                    }

                    // Handle Bonus Stats
                    if (bonusItemStat.isForStats()) {
                        this.getStats(AccessoryData.Type.STATS)
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

        AccessoryData that = (AccessoryData) o;

        return this.isBonusCalculated() == that.isBonusCalculated()
            && Objects.equals(this.getAccessory(), that.getAccessory());
    }

    @Override
    protected Type[] getAllTypes() {
        return AccessoryData.Type.values();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(this.getAccessory(), this.isBonusCalculated());
    }

    public final boolean isMissingEnrichment() {
        return this.getRarity().isEnrichable() && this.getEnrichmentStat().isEmpty();
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
