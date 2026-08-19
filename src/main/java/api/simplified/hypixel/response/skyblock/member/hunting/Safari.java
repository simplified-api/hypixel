package api.simplified.hypixel.response.skyblock.member.hunting;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import org.jetbrains.annotations.NotNull;

/**
 * A member's Safari record - the critters caught on each biome expedition, and the tickets that pay
 * for the next one.
 *
 * <p>
 * Safari is the Hunting expedition loop: a ticket buys a trip to one biome, the trip is spent
 * capturing the critters that live there, and each biome pays milestone rewards as its capture count
 * climbs. A critter caught for the first time joins the discovered list and stays there.
 *
 * <p>
 * The three maps are keyed by an enum rather than by the wire's own strings, so each descends into
 * its node as a catch-all: a key naming no constant is diverted to overflow and comes back on write,
 * rather than collapsing onto the one {@code null} entry every unmatched key would otherwise share.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Safari">Safari</a>
 */
@Getter
public class Safari {

    /**
     * Highest milestone tier claimed on each biome.
     */
    @Capture(descend = true)
    @SerializedName("milestone_claimed_tiers")
    private @NotNull ConcurrentMap<Biome, Integer> claimedMilestoneTiers = Concurrent.newMap();

    /**
     * Expedition tickets held, keyed by grade. A grade the member holds none of is sent as a zero
     * rather than left out.
     */
    @Capture(descend = true)
    private @NotNull ConcurrentMap<Ticket, Integer> tickets = Concurrent.newMap();

    /**
     * Critters captured on each biome. This is the running total the milestones are paid against,
     * not the number of distinct critters found there.
     */
    @Capture(descend = true)
    @SerializedName("biome_captures")
    private @NotNull ConcurrentMap<Biome, Integer> biomeCaptures = Concurrent.newMap();

    /**
     * Ids of the critters the member has captured at least once.
     */
    @SerializedName("discovered_critters")
    private @NotNull ConcurrentList<String> discoveredCritters = Concurrent.newList();

    /**
     * Ids of the critters the member has captured in their sparkling variant, a subset of the
     * discovered critters.
     */
    @SerializedName("discovered_sparkling_critters")
    private @NotNull ConcurrentList<String> discoveredSparklingCritters = Concurrent.newList();

    /**
     * Sparkling critters captured in total, counting every capture rather than every id.
     */
    @SerializedName("total_captured_sparkling_critters")
    private int capturedSparklingCritters;

    /**
     * The biomes a Safari expedition travels to, each holding its own critters and paying its own
     * milestones.
     *
     * <p>
     * The wire spells each lowercase and binds case-insensitively. Used as a map key a biome writes
     * back as its constant name, so that spelling does not round-trip.
     */
    public enum Biome {

        /**
         * The underground biome.
         */
        CAVERN,

        /**
         * The woodland biome.
         */
        FOREST,

        /**
         * The haunted biome.
         */
        HAUNTED,

        /**
         * The frozen biome.
         */
        ICY

    }

    /**
     * The grades of Safari expedition ticket, named the way a carrier names a cabin and climbing in
     * that order.
     *
     * <p>
     * The wire spells each lowercase and binds case-insensitively. Used as a map key a grade writes
     * back as its constant name, so that spelling does not round-trip.
     */
    public enum Ticket {

        /**
         * The cheapest grade.
         */
        ECONOMY,

        /**
         * The standard grade.
         */
        BASIC,

        /**
         * The grade above standard.
         */
        PREMIUM,

        /**
         * The dearest grade.
         */
        FIRST_CLASS

    }

}
