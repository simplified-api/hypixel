package api.simplified.hypixel.response.skyblock.member.crimson;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.model.Zone;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The trophy fish of the Crimson Isle, each with its display name, its rarity and, where the catch is
 * zone-gated, the id of the zone the bobber must sit in.
 * <p>
 * A zone is only ever part of a catch requirement. Baits, rods, mana floors, block proximity and timing
 * windows gate several of these fish and none of that is modelled here, so a constant carrying no zone
 * is not thereby unconditional.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Trophy_Fish">Trophy Fish</a>
 */
@Getter
@RequiredArgsConstructor
public enum TrophyFish {

    /**
     * Blobfish, common, with no zone set on it.
     */
    BLOBFISH("Blobfish", Rarity.COMMON),

    /**
     * Gusher, common, with the Blazing Volcano set as its zone.
     */
    GUSHER("Gusher", Rarity.COMMON, "BLAZING_VOLCANO"),

    /**
     * Obfuscated 1, common, with no zone set on it - the first link of the Obfuscated chain, which is a
     * bait requirement rather than a place. The wire spells it {@code obfuscated_fish_1}.
     */
    OBFUSCATED_FISH_1("Obfuscated 1", Rarity.COMMON),

    /**
     * Steaming-Hot Flounder, common, with the Blazing Volcano set as its zone.
     */
    STEAMING_HOT_FLOUNDER("Steaming-Hot Flounder", Rarity.COMMON, "BLAZING_VOLCANO"),

    /**
     * Sulphur Skitter, common, with no zone set on it - what gates it is standing near sulphur rather
     * than being anywhere in particular.
     */
    SULPHUR_SKITTER("Sulphur Skitter", Rarity.COMMON),

    /**
     * Flyfish, uncommon, with no zone set on it.
     */
    FLYFISH("Flyfish", Rarity.UNCOMMON),

    /**
     * Obfuscated 2, uncommon, with no zone set on it - caught using Obfuscated 1 as bait.
     */
    OBFUSCATED_FISH_2("Obfuscated 2", Rarity.UNCOMMON),

    /**
     * Slugfish, uncommon, with no zone set on it - what gates it is how long the rod sits out.
     */
    SLUGFISH("Slugfish", Rarity.UNCOMMON),

    /**
     * Lava Horse, rare, with no zone set on it. The wire spells it {@code lava_horse} and the display
     * name here is the spelling this class owns.
     */
    LAVA_HORSE("Lava Horse", Rarity.RARE),

    /**
     * Mana Ray, rare, with no zone set on it - what gates it is the mana the player is carrying.
     */
    MANA_RAY("Mana Ray", Rarity.RARE),

    /**
     * Obfuscated 3, rare, with no zone set on it - the last link of the Obfuscated chain, caught using
     * Obfuscated 2 as bait.
     */
    OBFUSCATED_FISH_3("Obfuscated 3", Rarity.RARE),

    /**
     * Vanille, rare, with no zone set on it - what gates it is the rod in the player's hand.
     */
    VANILLE("Vanille", Rarity.RARE),

    /**
     * Volcanic Stonefish, rare, with the Blazing Volcano set as its zone.
     */
    VOLCANIC_STONEFISH("Volcanic Stonefish", Rarity.RARE, "BLAZING_VOLCANO"),

    /**
     * Karate Fish, epic, with the Dojo set as its zone.
     */
    KARATE_FISH("Karate Fish", Rarity.EPIC, "DOJO"),

    /**
     * Moldfin, epic, with the Mystic Marsh set as its zone - one field, and not the only place the fish
     * can be pulled out of.
     */
    MOLDFIN("Moldfin", Rarity.EPIC, "MYSTIC_MARSH"),

    /**
     * Skeleton Fish, epic, with the Burning Desert set as its zone - one field, and not the only place
     * the fish can be pulled out of.
     */
    SKELETON_FISH("Skeleton Fish", Rarity.EPIC, "BURNING_DESERT"),

    /**
     * Soul Fish, epic, with the Stronghold set as its zone.
     */
    SOUL_FISH("Soul Fish", Rarity.EPIC, "STRONGHOLD"),

    /**
     * Golden Fish, legendary, with no zone set on it - it surfaces after a stretch of continuous
     * fishing rather than in a place.
     */
    GOLDEN_FISH("Golden Fish", Rarity.LEGENDARY);

    /**
     * The fish's name as this class spells it, which is neither the wire's id nor always the spelling
     * used elsewhere.
     */
    private final @NotNull String displayName;

    /**
     * The fish's rarity.
     */
    private final @NotNull Rarity rarity;

    /**
     * The id of the zone the bobber must sit in, empty where no zone gates the catch. It is a partial
     * index into the requirement and never carries a bait, a rod or a timing window.
     */
    private final @NotNull Optional<String> zoneId;

    /**
     * Constructs a fish with no zone gating its catch.
     *
     * @param displayName the fish's display name
     * @param rarity the fish's rarity
     */
    TrophyFish(@NotNull String displayName, @NotNull Rarity rarity) {
        this(displayName, rarity, Optional.empty());
    }

    /**
     * Constructs a fish whose bobber has to sit in a named zone.
     *
     * @param displayName the fish's display name
     * @param rarity the fish's rarity
     * @param zoneId the id of the zone the bobber must sit in
     */
    TrophyFish(@NotNull String displayName, @NotNull Rarity rarity, @NotNull String zoneId) {
        this(displayName, rarity, Optional.of(zoneId));
    }

    /**
     * The {@link Zone} the fish's id points at, resolved through the zone repository. This is the one
     * resolved member in the Crimson Isle tree, so it needs an open data session; it is empty where the
     * fish carries no zone id at all.
     */
    public @NotNull Optional<Zone> getZone() {
        return this.getZoneId().map(zoneId -> SkyBlockData.getRepository(Zone.class).findFirstOrNull(Zone::getId, zoneId));
    }

    /**
     * The four grades a trophy fish is caught at, each worth more Magmafish when filleted.
     * <p>
     * The wire spells these lowercase in both places they appear and the lookup is case-insensitive,
     * but an enum written back comes out as its constant name, so case does not round-trip. There is no
     * "no tier" constant and none is needed - a key with no tier suffix is the fish's total instead.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Trophy_Fish">Trophy Fish</a>
     */
    public enum Tier {

        /**
         * The guaranteed grade - every trophy fish caught is at least Bronze.
         */
        BRONZE,

        /**
         * The second grade, roughly a quarter of catches.
         */
        SILVER,

        /**
         * A rare grade, guaranteed after 100 catches of that fish without one.
         */
        GOLD,

        /**
         * The rarest grade, guaranteed after 600 catches of that fish without one.
         */
        DIAMOND

    }

}
