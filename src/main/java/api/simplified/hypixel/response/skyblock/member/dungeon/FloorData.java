package api.simplified.hypixel.response.skyblock.member.dungeon;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import dev.simplified.gson.annotation.Lenient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * One difficulty's record across all eight Catacombs floors.
 * <p>
 * Every member here except the experience total and the highest cleared floor is a map from a
 * {@link Floor} to that floor's number - how often it was entered and cleared, the fastest and
 * highest-scoring clears, and the per-class damage bests.
 * <p>
 * <b>Every one of those maps except the best runs carries one extra wire entry that is not a
 * floor</b> - {@code total} on the counters and {@code best} on the records. It is neither the sum
 * nor the maximum of the per-floor entries and cannot be recomputed from them, so it is best treated
 * as an upstream number of its own. {@link Floor} names no constant for either spelling, so each of
 * those maps takes the entry into overflow rather than binding it: what a caller walks is floors and
 * nothing else, and the entry is restored verbatim on write.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Catacombs">Catacombs</a>
 */
@Getter
@NoArgsConstructor
public class FloorData {

    /**
     * Catacombs experience earned on this difficulty.
     * <p>
     * Master mode sends no such key at all, so the master record holds zero and the pair reads its
     * experience off the normal side.
     */
    protected double experience;

    /**
     * The highest floor number the member has cleared on this difficulty.
     */
    @SerializedName("highest_tier_completed")
    private int highestCompletedTier;

    /**
     * Up to ten best runs on each floor.
     * <p>
     * The one floor-keyed map here with no aggregate entry, so its keys are pure floor digits.
     */
    @SerializedName("best_runs")
    private @NotNull ConcurrentMap<Floor, ConcurrentList<BestRun>> bestRuns = Concurrent.newMap();

    /**
     * Runs entered on each floor, cleared or not.
     */
    @Lenient
    @SerializedName("times_played")
    private @NotNull ConcurrentMap<Floor, Integer> timesPlayed = Concurrent.newMap();

    /**
     * Runs completed on each floor, always at or below the runs entered.
     * <p>
     * Unrecognised entries fall into overflow rather than failing, which is what catches the wire's
     * non-floor aggregate key and restores it verbatim on write.
     */
    @Lenient
    @SerializedName("tier_completions")
    private @NotNull ConcurrentMap<Floor, Integer> completions = Concurrent.newMap();

    /**
     * Completions in which the member reached a class milestone, always at or below the completions.
     * <p>
     * A milestone measures the member's contribution to the run - damage dealt for Berserk and Mage,
     * ranged damage for Archer, damage tanked and dealt for Tank, healing for Healer - nine per
     * class, with higher floors demanding more of each.
     */
    @Lenient
    @SerializedName("milestone_completions")
    private @NotNull ConcurrentMap<Floor, Integer> milestoneCompletions = Concurrent.newMap();

    /**
     * Highest Dungeon Score reached on each floor, 300 and above being an S+.
     */
    @Lenient
    @SerializedName("best_score")
    private @NotNull ConcurrentMap<Floor, Integer> bestScore = Concurrent.newMap();

    /**
     * Times The Watcher was killed on each floor - he is the boss of the Entrance and the Blood Room
     * mini-boss on every other floor.
     */
    @Lenient
    @SerializedName("watcher_kills")
    private @NotNull ConcurrentMap<Floor, Integer> watcherKills = Concurrent.newMap();

    /**
     * Lifetime mobs killed on each floor.
     */
    @Lenient
    @SerializedName("mobs_killed")
    private @NotNull ConcurrentMap<Floor, Integer> mobsKilled = Concurrent.newMap();

    /**
     * Most mobs killed in a single run on each floor.
     */
    @Lenient
    @SerializedName("most_mobs_killed")
    private @NotNull ConcurrentMap<Floor, Integer> mostMobsKilled = Concurrent.newMap();

    /**
     * Most health healed to allies in a single run on each floor.
     */
    @Lenient
    @SerializedName("most_healing")
    private @NotNull ConcurrentMap<Floor, Double> mostHealing = Concurrent.newMap();

    // Class Damage

    /**
     * Most damage dealt in a single run, keyed first by the class that dealt it and then by floor.
     * <p>
     * The filter folds the five {@code most_damage_<class>} wire keys into this one map. It is
     * sparse - only a class the member actually played on a floor appears - and because an enum map
     * key writes as the constant name, the wire's lowercase suffix comes back uppercase.
     */
    @Capture(filter = "^most_damage_")
    private @NotNull ConcurrentMap<DungeonClass.Type, ConcurrentMap<Floor, Double>> mostDamage = Concurrent.newMap();

    // Fastest Times

    /**
     * Fastest clear of each floor, in milliseconds.
     */
    @Lenient
    @SerializedName("fastest_time")
    private @NotNull ConcurrentMap<Floor, Integer> fastestTime = Concurrent.newMap();

    /**
     * Fastest clear of each floor that scored S, 270 and above, in milliseconds.
     */
    @Lenient
    @SerializedName("fastest_time_s")
    private @NotNull ConcurrentMap<Floor, Integer> fastestSTierTime = Concurrent.newMap();

    /**
     * Fastest clear of each floor that scored S+, 300 and above, in milliseconds.
     * <p>
     * It can be slower than the S record on the same floor - the two are separate records rather
     * than nested thresholds.
     */
    @Lenient
    @SerializedName("fastest_time_s_plus")
    private @NotNull ConcurrentMap<Floor, Integer> fastestSPlusTierTime = Concurrent.newMap();

    /**
     * Reads one class's per-floor damage bests out of the captured map.
     *
     * @param classType the class that dealt the damage
     * @return an unmodifiable map of floor to best damage, empty rather than null for a class the
     * wire sent nothing for
     */
    public @NotNull ConcurrentMap<Floor, Double> getMostDamage(@NotNull DungeonClass.Type classType) {
        return this.getMostDamage()
            .getOrDefault(classType, Concurrent.newMap())
            .toUnmodifiable();
    }

    /**
     * One of the ten highest-scoring runs the member has completed on a floor.
     * <p>
     * Dungeon Score is granted at the end of a run out of exploration, speed, skill and bonus
     * points; a higher score allows more reward chests to be claimed and pays more Catacombs and
     * class experience. There is no total field - the score is those four components summed.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Dungeon_Score">Dungeon Score</a>
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BestRun {

        // Time

        /**
         * When the run finished, in real time.
         */
        private SkyBlockDate.RealTime timestamp;

        /**
         * How long the run took, in milliseconds.
         */
        @SerializedName("elapsed_time")
        private int elapsedTime;

        // Score

        /**
         * Up to 100 points - 60 scaled by the fraction of rooms cleared, plus up to 40 for secrets
         * found against the floor's secret requirement.
         */
        @SerializedName("score_exploration")
        private int explorationScore;

        /**
         * Up to 100 points, decaying once the floor's time allowance is spent. On a failed run it is
         * the percentage of rooms cleared instead.
         */
        @SerializedName("score_speed")
        private int speedScore;

        /**
         * 100 points less 2 for every death and 14 for every failed puzzle.
         */
        @SerializedName("score_skill")
        private int skillScore;

        /**
         * Up to 5 points for Crypts blown up and cleared, plus 2 for killing the Mimic on floor 6
         * and above, plus 10 while mayor Paul's EZPZ perk is active.
         */
        @SerializedName("score_bonus")
        private int bonusScore;

        // Damage

        /**
         * Damage the member dealt over the run.
         */
        @SerializedName("damage_dealt")
        private double damageDealt;

        /**
         * Damage the member soaked, the Tank contribution.
         */
        @SerializedName("damage_mitigated")
        private double damageMitigated;

        /**
         * Health restored to teammates, the Healer contribution.
         * <p>
         * The wire leaves it off a good share of runs, so it is often the zero default rather than a
         * bound value.
         */
        @SerializedName("ally_healing")
        private double allyHealing;

        /**
         * The class the member played that run.
         */
        @SerializedName("dungeon_class")
        private @NotNull DungeonClass.Type dungeonClass = DungeonClass.Type.UNKNOWN;

        /**
         * The other party members' player uuids.
         * <p>
         * The one collection in this package with no default, so it is null when the wire omits it
         * rather than an empty list.
         */
        private ConcurrentList<UUID> teammates;

        /**
         * Deaths during the run, each costing 2 skill points.
         */
        @SerializedName("deaths")
        private int deaths;

        /**
         * Mobs the member killed that run.
         */
        @SerializedName("mobs_killed")
        private int mobsKilled;

        /**
         * Dungeon secrets the member personally found that run.
         */
        @SerializedName("secrets_found")
        private int secretsFound;

    }

}
