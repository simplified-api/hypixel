package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.dungeon;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.SimplifiedApi;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.Experience;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.weight.Weight;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.weight.Weighted;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.data.model.dungeon_data.dungeon_classes.DungeonClassModel;
import dev.sbs.minecraftapi.data.model.dungeon_data.dungeon_levels.DungeonLevelModel;
import dev.sbs.minecraftapi.data.model.dungeon_data.dungeons.DungeonModel;
import dev.sbs.api.util.NumberUtil;
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

    /**
     * Wraps this class in a {@link Experience} and {@link Weight} class.
     * <br><br>
     * Requires an active database session.
     */
    public @NotNull EnhancedDungeon asEnhanced() {
        return new EnhancedDungeon(this);
    }

    public @NotNull ConcurrentMap<Integer, Double> getMostDamage(@NotNull Class.Type classType) {
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

        /**
         * Gets the {@link DungeonModel} for the given {@link Type}.
         * <br><br>
         * Requires an active database session.
         */
        public @NotNull DungeonModel getModel() {
            if (this == UNKNOWN)
                throw new UnsupportedOperationException("Unknown does not exist in the database!");

            return SimplifiedApi.getRepositoryOf(DungeonModel.class).findFirstOrNull(DungeonModel::getKey, this.name());
        }

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

        public DungeonClassModel getDungeonClass() {
            return SimplifiedApi.getRepositoryOf(DungeonClassModel.class).findFirstOrNull(DungeonClassModel::getKey, this.dungeonClass.toUpperCase());
        }

    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    public static class Class {

        private final @NotNull Type type;
        private final double experience;

        /**
         * Wraps this class in a {@link Experience} and {@link Weight} class.
         * <br><br>
         * Requires an active database session.
         */
        public @NotNull Dungeon.EnhancedClass asEnhanced() {
            return new EnhancedClass(this);
        }

        public enum Type {

            UNKNOWN,
            HEALER,
            MAGE,
            BERSERK,
            ARCHER,
            TANK;

            /**
             * Gets the {@link DungeonClassModel} for the given {@link Type}.
             * <br><br>
             * Requires an active database session.
             */
            public @NotNull DungeonClassModel getModel() {
                if (this == UNKNOWN)
                    throw new UnsupportedOperationException("Unknown does not exist in the database!");

                return SimplifiedApi.getRepositoryOf(DungeonClassModel.class).findFirstOrNull(DungeonClassModel::getKey, this.name());
            }

            public static @NotNull Type of(@NotNull String name) {
                return Arrays.stream(values())
                    .filter(type -> type.name().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(UNKNOWN);
            }

        }

    }

    @Getter
    public static class EnhancedClass extends Class implements Experience, Weighted {

        private final @NotNull DungeonClassModel typeModel;
        private final @NotNull ConcurrentList<Double> experienceTiers;

        private EnhancedClass(@NotNull Class dungeonClass) {
            super(dungeonClass.getType(), dungeonClass.getExperience());
            this.typeModel = this.getType().getModel();
            this.experienceTiers = SimplifiedApi.getRepositoryOf(DungeonLevelModel.class)
                .stream()
                .map(DungeonLevelModel::getTotalExpRequired)
                .collect(Concurrent.toList());
        }

        @Override
        public int getMaxLevel() {
            return this.getExperienceTiers().size();
        }

        @Override
        public @NotNull Weight getWeight() {
            double rawLevel = this.getRawLevel();
            ConcurrentList<Double> experienceTiers = this.getExperienceTiers();
            double maxDungeonClassExperienceRequired = experienceTiers.get(experienceTiers.size() - 1);

            if (rawLevel < this.getMaxLevel())
                rawLevel += (this.getProgressPercentage() / 100); // Add Percentage Progress to Next Level

            double base = Math.pow(rawLevel, 4.5) * this.getTypeModel().getWeightMultiplier();
            double weightValue = NumberUtil.round(base, 2);
            double weightOverflow = 0;

            if (this.getExperience() > maxDungeonClassExperienceRequired) {
                double overflow = Math.pow((this.getExperience() - maxDungeonClassExperienceRequired) / (4 * maxDungeonClassExperienceRequired / base), 0.968);
                weightOverflow = NumberUtil.round(overflow, 2);
            }

            return Weight.of(weightValue, weightOverflow);
        }

    }

}
