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

@Getter
@NoArgsConstructor
public class FloorData {

    protected double experience;
    @SerializedName("highest_tier_completed")
    private int highestCompletedTier;
    @SerializedName("best_runs")
    private @NotNull ConcurrentMap<Floor, ConcurrentList<BestRun>> bestRuns = Concurrent.newMap();

    @SerializedName("times_played")
    private @NotNull ConcurrentMap<Floor, Integer> timesPlayed = Concurrent.newMap();
    @Lenient
    @SerializedName("tier_completions")
    private @NotNull ConcurrentMap<Floor, Integer> completions = Concurrent.newMap();
    @SerializedName("milestone_completions")
    private @NotNull ConcurrentMap<Floor, Integer> milestoneCompletions = Concurrent.newMap();

    @SerializedName("best_score")
    private @NotNull ConcurrentMap<Floor, Integer> bestScore = Concurrent.newMap();
    @SerializedName("watcher_kills")
    private @NotNull ConcurrentMap<Floor, Integer> watcherKills = Concurrent.newMap();
    @SerializedName("mobs_killed")
    private @NotNull ConcurrentMap<Floor, Integer> mobsKilled = Concurrent.newMap();
    @SerializedName("most_mobs_killed")
    private @NotNull ConcurrentMap<Floor, Integer> mostMobsKilled = Concurrent.newMap();
    @SerializedName("most_healing")
    private @NotNull ConcurrentMap<Floor, Double> mostHealing = Concurrent.newMap();

    // Class Damage
    @Capture(filter = "^most_damage_")
    private @NotNull ConcurrentMap<DungeonClass.Type, ConcurrentMap<Floor, Double>> mostDamage = Concurrent.newMap();

    // Fastest Times
    @SerializedName("fastest_time")
    private @NotNull ConcurrentMap<Floor, Integer> fastestTime = Concurrent.newMap();
    @SerializedName("fastest_time_s")
    private @NotNull ConcurrentMap<Floor, Integer> fastestSTierTime = Concurrent.newMap();
    @SerializedName("fastest_time_s_plus")
    private @NotNull ConcurrentMap<Floor, Integer> fastestSPlusTierTime = Concurrent.newMap();

    public @NotNull ConcurrentMap<Floor, Double> getMostDamage(@NotNull DungeonClass.Type classType) {
        return this.getMostDamage()
            .getOrDefault(classType, Concurrent.newMap())
            .toUnmodifiable();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BestRun {

        // Time
        private SkyBlockDate.RealTime timestamp;
        @SerializedName("elapsed_time")
        private int elapsedTime;

        // Score
        @SerializedName("score_exploration")
        private int explorationScore;
        @SerializedName("score_speed")
        private int speedScore;
        @SerializedName("score_skill")
        private int skillScore;
        @SerializedName("score_bonus")
        private int bonusScore;

        // Damage
        @SerializedName("damage_dealt")
        private double damageDealt;
        @SerializedName("damage_mitigated")
        private double damageMitigated;
        @SerializedName("ally_healing")
        private double allyHealing;

        @SerializedName("dungeon_class")
        private @NotNull DungeonClass.Type dungeonClass = DungeonClass.Type.UNKNOWN;
        private ConcurrentList<UUID> teammates;
        @SerializedName("deaths")
        private int deaths;
        @SerializedName("mobs_killed")
        private int mobsKilled;
        @SerializedName("secrets_found")
        private int secretsFound;

    }

}
