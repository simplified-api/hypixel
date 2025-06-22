package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.dungeon;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Dungeon {

    protected @NotNull Type type;
    protected double experience;
    @SerializedName("highest_tier_completed")
    protected int highestCompletedTier;
    @SerializedName("best_runs")
    protected @NotNull ConcurrentMap<Integer, ConcurrentList<BestRun>> bestRuns = Concurrent.newMap();

    @SerializedName("times_played")
    protected @NotNull ConcurrentMap<Integer, Integer> timesPlayed = Concurrent.newMap();
    @SerializedName("tier_completions")
    protected @NotNull ConcurrentMap<Integer, Integer> completions = Concurrent.newMap();
    @SerializedName("milestone_completions")
    protected @NotNull ConcurrentMap<Integer, Integer> milestoneCompletions = Concurrent.newMap();

    @SerializedName("best_score")
    protected @NotNull ConcurrentMap<Integer, Integer> bestScore = Concurrent.newMap();
    @SerializedName("watcher_kills")
    protected @NotNull ConcurrentMap<Integer, Integer> watcherKills = Concurrent.newMap();
    @SerializedName("mobs_killed")
    protected @NotNull ConcurrentMap<Integer, Integer> mobsKilled = Concurrent.newMap();
    @SerializedName("most_mobs_killed")
    protected @NotNull ConcurrentMap<Integer, Integer> mostMobsKilled = Concurrent.newMap();
    @SerializedName("most_healing")
    protected @NotNull ConcurrentMap<Integer, Double> mostHealing = Concurrent.newMap();

    // Class Damage
    @Getter(AccessLevel.NONE)
    @SerializedName("most_damage_healer")
    protected @NotNull ConcurrentMap<Integer, Double> mostDamageHealer = Concurrent.newMap();
    @Getter(AccessLevel.NONE)
    @SerializedName("most_damage_mage")
    protected @NotNull ConcurrentMap<Integer, Double> mostDamageMage = Concurrent.newMap();
    @Getter(AccessLevel.NONE)
    @SerializedName("most_damage_berserk")
    protected @NotNull ConcurrentMap<Integer, Double> mostDamageBerserk = Concurrent.newMap();
    @Getter(AccessLevel.NONE)
    @SerializedName("most_damage_archer")
    protected @NotNull ConcurrentMap<Integer, Double> mostDamageArcher = Concurrent.newMap();
    @Getter(AccessLevel.NONE)
    @SerializedName("most_damage_tank")
    protected @NotNull ConcurrentMap<Integer, Double> mostDamageTank = Concurrent.newMap();

    // Fastest Times
    @SerializedName("fastest_time")
    protected @NotNull ConcurrentMap<Integer, Integer> fastestTime = Concurrent.newMap();
    @SerializedName("fastest_time_s")
    protected @NotNull ConcurrentMap<Integer, Integer> fastestSTierTime = Concurrent.newMap();
    @SerializedName("fastest_time_s_plus")
    protected @NotNull ConcurrentMap<Integer, Integer> fastestSPlusTierTime = Concurrent.newMap();

    public @NotNull ConcurrentMap<Integer, Double> getMostDamage(@NotNull DungeonClass.Type classType) {
        return switch (classType) {
            case HEALER -> this.mostDamageHealer.toUnmodifiableMap();
            case MAGE -> this.mostDamageMage.toUnmodifiableMap();
            case BERSERK -> this.mostDamageBerserk.toUnmodifiableMap();
            case ARCHER -> this.mostDamageArcher.toUnmodifiableMap();
            case TANK -> this.mostDamageTank.toUnmodifiableMap();
            default -> Concurrent.newUnmodifiableMap();
        };
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum Type {

        UNKNOWN(false),
        CATACOMBS(true),
        MASTER_CATACOMBS(false);

        @Accessors(fluent = true)
        private final boolean containsExperience;

        /*public @NotNull DungeonModel getModel() {
            if (this == UNKNOWN)
                throw new UnsupportedOperationException("Unknown does not exist in the database!");

            return SimplifiedApi.getRepositoryOf(DungeonModel.class).findFirstOrNull(DungeonModel::getKey, this.name());
        }*/

        public boolean isMasterMode() {
            return this == MASTER_CATACOMBS;
        }

        public static @NotNull Dungeon.Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BestRun {

        // Time
        @Getter private SkyBlockDate.RealTime timestamp;
        @SerializedName("elapsed_time")
        @Getter private int elapsedTime;

        // Score
        @SerializedName("score_exploration")
        @Getter private int explorationScore;
        @SerializedName("score_speed")
        @Getter private int speedScore;
        @SerializedName("score_skill")
        @Getter private int skillScore;
        @SerializedName("score_bonus")
        @Getter private int bonusScore;

        // Damage
        @SerializedName("damage_dealt")
        @Getter private double damageDealt;
        @SerializedName("damage_mitigated")
        @Getter private double damageMitigated;
        @SerializedName("ally_healing")
        @Getter private double allyHealing;

        @SerializedName("dungeon_class")
        private String dungeonClass;
        @Getter private ConcurrentList<UUID> teammates;
        @SerializedName("deaths")
        @Getter private int deaths;
        @SerializedName("mobs_killed")
        @Getter private int mobsKilled;
        @SerializedName("secrets_found")
        @Getter private int secretsFound;

        /*public DungeonClassModel getDungeonClass() {
            return SimplifiedApi.getRepositoryOf(DungeonClassModel.class).findFirstOrNull(DungeonClassModel::getKey, this.dungeonClass.toUpperCase());
        }*/

    }

}
