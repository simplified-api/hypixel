package api.simplified.hypixel.profile_stats.data;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.model.Accessory;
import api.simplified.skyblock.model.BonusItemStat;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lib.minecraft.nbt.NbtFactory;
import lib.minecraft.nbt.exception.NbtException;
import lib.minecraft.nbt.tag.ByteArrayTag;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.IntTag;
import lib.minecraft.nbt.tag.ListTag;
import lib.minecraft.nbt.tag.StringTag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Optional;

/**
 * One accessory out of a member's bag, with everything it contributes already totalled.
 * <p>
 * Only what is fixed about the accessory is read here - its own stats, its gemstones, its enrichment
 * and the New Year Cake Bag's per-cake health. Anything conditional on the rest of the player waits
 * for {@link #calculateBonus(ConcurrentMap)}.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Accessories">Accessories</a>
 */
@Getter
public class AccessoryData extends ObjectData<AccessoryData.Type> {

    private static final ConcurrentList<Integer> PULSE_CHARGES = Concurrent.newList(150_000, 1_000_000, 5_000_000);

    /**
     * Reference data for the accessory this instance is of.
     */
    private final Accessory accessory;

    /**
     * Whether the conditional bonuses have already been evaluated.
     */
    private boolean bonusCalculated;

    /**
     * Stat the accessory has been enriched toward, empty when it carries no enrichment.
     */
    private final Optional<Stat> enrichmentStat;

    /**
     * Constructs a new {@code AccessoryData} and totals everything the accessory gives outright.
     *
     * @param accessory reference data for the accessory being read
     * @param compoundTag the accessory's NBT tag
     */
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

    /**
     * Whether the accessory is rare enough to be enriched but carries no enrichment.
     */
    public final boolean isMissingEnrichment() {
        return this.getRarity().isEnrichable() && this.getEnrichmentStat().isEmpty();
    }

    /**
     * The four sources an accessory's stats are split between.
     * <p>
     * All four are fixed for a given accessory, so an optimiser can total them once and reuse the
     * result across every candidate loadout.
     */
    @Getter
    @RequiredArgsConstructor
    public enum Type implements ObjectData.Type {

        /**
         * Health from the New Year Cake Bag, one point for each cake stored in it.
         */
        CAKE_BAG(true),

        /**
         * Stats from the gemstones slotted into the accessory.
         */
        GEMSTONES(true),

        /**
         * Stats the accessory provides in its own right.
         */
        STATS(true),

        /**
         * The single stat an accessory enrichment adds.
         */
        ENRICHMENTS(true);

        /**
         * Whether this source is fixed for the accessory, so an optimiser need not recompute it.
         */
        private final boolean optimizerConstant;

    }

}
