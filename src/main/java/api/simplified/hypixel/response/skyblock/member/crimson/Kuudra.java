package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.PairOptional;
import dev.simplified.gson.annotation.Capture;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Split;
import dev.simplified.util.Range;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A member's record against Kuudra, the boss reached through the Forgotten Skull and fought in
 * Kuudra's Hollow.
 * <p>
 * The wire node is named {@code kuudra_completed_tiers}, but it holds both halves of the record - a
 * clear count per tier and a highest wave reached per tier are siblings inside it - so this class
 * models the whole node rather than only the completions its key names.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Kuudra">Kuudra</a>
 */
@Getter
public class Kuudra {

    /**
     * The highest wave the member reached on each tier, gathered from every {@code highest_wave_} key
     * with the prefix stripped. A tier with no entry is normal rather than an error.
     */
    @Capture(filter = "^highest_wave_")
    private @NotNull ConcurrentMap<Tier, Integer> highestWave = Concurrent.newMap();

    /**
     * How many times the member has cleared each tier, gathered from every remaining key. This is the
     * unfiltered catch-all, so a key that is not a tier name is diverted to overflow rather than bound
     * onto a {@code null} entry.
     */
    @Capture
    private @NotNull ConcurrentMap<Tier, Integer> completedTiers = Concurrent.newMap();

    /**
     * The keys the catch-all could not turn into a {@link Tier}, read back out of its overflow under
     * the spelling the wire gave them. Without the diversion every unmatched key would collapse onto
     * one {@code null} entry and all but the last would be lost.
     */
    @Extract("completedTiers")
    private @NotNull ConcurrentMap<String, Integer> unknownTiers = Concurrent.newMap();

    /**
     * Kuudra's five difficulty tiers.
     * <p>
     * Each is gated on beating the one below it and on faction reputation, and the wave timer tightens
     * as the tiers climb. Used as a map key a tier writes back as its constant name, so the wire's
     * lowercase spelling does not round-trip.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Kuudra">Kuudra</a>
     */
    @Getter
    public enum Tier {

        /**
         * The entry tier, reached by being taken to Kuudra rather than by reputation. It is the one
         * constant renamed away from the wire, which spells it {@code none} and binds
         * case-insensitively.
         */
        @SerializedName("NONE")
        BASIC,

        /**
         * The second tier, gated on the main quest and 1,000 faction reputation.
         */
        HOT,

        /**
         * The third tier, gated on 3,000 faction reputation; the stomach phase starts here.
         */
        BURNING,

        /**
         * The fourth tier, gated on 7,000 faction reputation.
         */
        FIERY,

        /**
         * The highest tier, gated on 12,000 faction reputation, and the one that adds the final lair
         * phase.
         */
        INFERNAL

    }

    /**
     * The filters a member last set in the Kuudra party finder while browsing other people's groups.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Party_Finder">Party Finder</a>
     */
    @Getter
    public static class SearchSettings {

        /**
         * Which Kuudra tier to list groups for.
         */
        private @NotNull Kuudra.Tier tier = Kuudra.Tier.BASIC;

        /**
         * Free-text filter over the group notes. An empty string is a present filter and is not the
         * same as no filter set.
         */
        private @NotNull Optional<String> search = Optional.empty();

        /**
         * How the listed groups are ordered.
         */
        private @NotNull Kuudra.SearchSettings.Sort sort = Kuudra.SearchSettings.Sort.RECENTLY_CREATED;

        /**
         * The two halves of the combat level filter, parsed out of the wire's delimited string by
         * {@link Split} rather than by hand. Not exposed as a pair - {@link #getCombatLevel()} reads it
         * as a range.
         */
        @Getter(AccessLevel.NONE)
        @SerializedName("combat_level")
        @Split("-")
        private @NotNull PairOptional<Integer, Integer> combatLevel = PairOptional.empty();

        /**
         * The combat level range the member is filtering on, derived from the parsed pair and falling
         * back to {@code 0..60} where the wire carried no usable range.
         */
        public @NotNull Range<Integer> getCombatLevel() {
            return this.combatLevel.map(Range::between).orElse(Range.between(0, 60));
        }

        /**
         * How the party finder orders the groups it lists.
         *
         * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Party_Finder">Party Finder</a>
         */
        public enum Sort {

            /**
             * Newest groups first, and the field's default.
             */
            RECENTLY_CREATED,

            /**
             * Groups asking the highest combat level first.
             */
            HIGHEST_COMBAT_LEVEL,

            /**
             * Fullest groups first.
             */
            LARGEST_GROUP_SIZE

        }

    }

    /**
     * The Kuudra group the member last advertised in the party finder - the tier they are running, the
     * note other players see, and the combat level they demand.
     * <p>
     * Alone in this package the class carries no non-null contract, though the note still defaults to
     * an empty {@link Optional}.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Party_Finder">Party Finder</a>
     */
    @Getter
    public static class GroupBuilder {

        /**
         * The Kuudra tier the advertised group is running.
         */
        private Kuudra.Tier tier = Kuudra.Tier.BASIC;

        /**
         * The blurb shown on the listing.
         */
        private Optional<String> note = Optional.empty();

        /**
         * The minimum combat level a player needs to join. The wire key here is
         * {@code combat_level_required} and holds a single number, so it is not interchangeable with
         * the range the search filters carry.
         */
        @SerializedName("combat_level_required")
        private int requiredCombatLevel;

    }

}
