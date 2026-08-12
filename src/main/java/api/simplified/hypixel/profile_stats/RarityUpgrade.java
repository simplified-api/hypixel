package api.simplified.hypixel.profile_stats;

import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.model.Item;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.IntTag;
import lib.minecraft.nbt.tag.StringTag;
import lib.minecraft.nbt.tag.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;

/**
 * A rarity upgrade only one item can carry, applied on top of the recombobulator and the tier boost.
 * <p>
 * Each reads the item's own tag and nothing else, so an item's rarity is settled before any reference
 * table is touched - which is what lets an accessory be counted and ranked without resolving a single
 * stat.
 */
@Getter
@RequiredArgsConstructor
public enum RarityUpgrade {

    /**
     * Seven perfect gemstones raise the Power Artifact one tier.
     */
    POWER_ARTIFACT("POWER_ARTIFACT") {

        @Override
        public int adjust(int rarityOrdinal, @NotNull CompoundTag compoundTag) {
            return rarityOrdinal + (countGemQuality(compoundTag, "PERFECT") == 7 ? 1 : 0);
        }

    },

    /**
     * Pandora's Box carries its own rarity in its tag, which replaces rather than raises.
     */
    PANDORAS_BOX("PANDORAS_BOX") {

        @Override
        public int adjust(int rarityOrdinal, @NotNull CompoundTag compoundTag) {
            String pandoraRarityKey = compoundTag.getPathOrDefault("tag.ExtraAttributes.pandora-rarity", StringTag.EMPTY).getValue().toUpperCase();

            try {
                return Rarity.valueOf(pandoraRarityKey).ordinal();
            } catch (IllegalArgumentException ignore) {
                return rarityOrdinal;
            }
        }

    },

    /**
     * Each thunder charge threshold the Pulse Ring has passed raises it one tier.
     */
    PULSE_RING("PULSE_RING") {

        @Override
        public int adjust(int rarityOrdinal, @NotNull CompoundTag compoundTag) {
            int thunderCharge = compoundTag.getPathOrDefault("tag.ExtraAttributes.thunder_charge", IntTag.EMPTY).getValue();

            return rarityOrdinal + (int) PULSE_CHARGES.stream()
                .filter(charge -> thunderCharge >= charge)
                .count();
        }

    },

    /**
     * Five hundred pelts raise the Trapper Crest one tier.
     */
    TRAPPER_CREST("TRAPPER_CREST") {

        @Override
        public int adjust(int rarityOrdinal, @NotNull CompoundTag compoundTag) {
            int pelts = compoundTag.getPathOrDefault("tag.ExtraAttributes.pelts_earned", IntTag.EMPTY).getValue();
            return rarityOrdinal + (pelts >= 500 ? 1 : 0);
        }

    };

    private static final ConcurrentList<Integer> PULSE_CHARGES = Concurrent.newUnmodifiableList(150_000, 1_000_000, 5_000_000);

    /**
     * Id of the item this upgrade belongs to.
     */
    private final @NotNull String itemId;

    /**
     * Adjusts the rarity for this item's own upgrade.
     *
     * @param rarityOrdinal the rarity already raised by a recombobulator and a tier boost
     * @param compoundTag the item's tag
     * @return the adjusted ordinal
     */
    public abstract int adjust(int rarityOrdinal, @NotNull CompoundTag compoundTag);

    /**
     * The rarity an item instance ends up at, after both generic upgrades and its own.
     *
     * @param itemModel reference data for the item being read
     * @param compoundTag the item's tag
     * @return the resolved rarity
     */
    public static @NotNull Rarity resolve(@NotNull Item itemModel, @NotNull CompoundTag compoundTag) {
        int rarityOrdinal = itemModel.getRarity().ordinal()
            + (isRecombobulated(compoundTag) ? 1 : 0)
            + (isTierBoosted(compoundTag) ? 1 : 0);

        return Rarity.of(apply(itemModel.getId(), rarityOrdinal, compoundTag));
    }

    /**
     * Applies whichever upgrade an item carries, if any.
     *
     * @param itemId the item being read
     * @param rarityOrdinal the rarity already raised by the generic upgrades
     * @param compoundTag the item's tag
     * @return the adjusted ordinal, unchanged for an item with no upgrade of its own
     */
    public static int apply(@NotNull String itemId, int rarityOrdinal, @NotNull CompoundTag compoundTag) {
        return Arrays.stream(values())
            .filter(upgrade -> upgrade.getItemId().equals(itemId))
            .findFirst()
            .map(upgrade -> upgrade.adjust(rarityOrdinal, compoundTag))
            .orElse(rarityOrdinal);
    }

    /**
     * Whether a recombobulator has raised the instance one rarity.
     *
     * @param compoundTag the item's tag
     * @return whether the tag records one
     */
    public static boolean isRecombobulated(@NotNull CompoundTag compoundTag) {
        return compoundTag.getPathOrDefault("tag.ExtraAttributes.rarity_upgrades", IntTag.EMPTY).getValue() == 1;
    }

    /**
     * Whether a tier boost has raised the instance one rarity.
     *
     * @param compoundTag the item's tag
     * @return whether the tag records one
     */
    public static boolean isTierBoosted(@NotNull CompoundTag compoundTag) {
        return compoundTag.getPathOrDefault("tag.ExtraAttributes.baseStatBoostPercentage", IntTag.EMPTY).getValue() > 0;
    }

    private static long countGemQuality(@NotNull CompoundTag compoundTag, @NotNull String quality) {
        return compoundTag.getPathOrDefault("tag.ExtraAttributes.gems", CompoundTag.EMPTY)
            .entrySet()
            .stream()
            .map(Map.Entry::getValue)
            .map(Tag::getValue)
            .filter(quality::equals)
            .count();
    }

}
