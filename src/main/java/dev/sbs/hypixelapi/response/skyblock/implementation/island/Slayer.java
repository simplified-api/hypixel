package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.SimplifiedApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.Experience;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.weight.Weight;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.weight.Weighted;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.data.model.slayer_levels.SlayerLevelModel;
import dev.sbs.minecraftapi.data.model.slayers.SlayerModel;
import dev.sbs.api.reflection.Reflection;
import dev.sbs.api.util.NumberUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public class Slayer {

    @SerializedName("slayer_quest")
    @Getter private @NotNull Optional<Quest> activeQuest = Optional.empty();
    @SerializedName("slayer_bosses")
    private @NotNull ConcurrentMap<Type, Boss> bossMap = Concurrent.newMap();
    private @NotNull ConcurrentList<Boss> bossList = Concurrent.newList();
    private boolean initialized;

    public @NotNull ConcurrentList<Boss> getBosses() {
        if (!this.initialized)
            this.initialize();

        return this.bossList;
    }

    public @NotNull Boss getBoss(@NotNull Type type) {
        return this.getBosses()
            .stream()
            .filter(boss -> boss.getType() == type)
            .findFirst()
            .orElseThrow();
    }

    private void initialize() {
        this.bossMap.forEach((type, boss) -> boss.type = type);
        this.bossList = Concurrent.newUnmodifiableList(this.bossMap.values());
        this.initialized = true;
    }

    @Getter
    public static class Quest {

        private Type type;
        private int tier;
        @SerializedName("start_timestamp")
        private Instant start;
        @SerializedName("completion_state")
        private int completionState;
        @SerializedName("used_armor")
        private boolean usedArmor;
        private boolean solo;

    }

    public enum Type {

        UNKNOWN,
        @SerializedName("ZOMBIE")
        REVENANT_HORROR,
        @SerializedName("SPIDER")
        TARANTULA_BROODFATHER,
        @SerializedName("WOLF")
        SVEN_PACKMASTER,
        @SerializedName("ENDERMAN")
        VOIDGLOOM_SERAPH,
        @SerializedName("BLAZE")
        INFERNO_DEMONLORD,
        @SerializedName("VAMPIRE")
        RIFTSTALKER_BLOODFIEND;

        /**
         * Gets the {@link SlayerModel} for the given {@link Type}.
         * <br><br>
         * Requires an active database session.
         */
        public @NotNull SlayerModel getModel() {
            if (this == UNKNOWN)
                throw new UnsupportedOperationException("Unknown does not exist in the database!");

            return SimplifiedApi.getRepositoryOf(SlayerModel.class).findFirstOrNull(SlayerModel::getKey, this.name());
        }

        public static @NotNull Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

    public static class Boss {

        private final static Reflection<Boss> slayerRef = Reflection.of(Boss.class);
        @Getter protected Type type;
        @SerializedName("xp")
        @Getter protected double experience;

        protected ConcurrentMap<String, Boolean> claimed_levels = Concurrent.newMap(); // level_#: true
        protected int boss_kills_tier_0;
        protected int boss_kills_tier_1;
        protected int boss_kills_tier_2;
        protected int boss_kills_tier_3;
        protected int boss_kills_tier_4;
        protected boolean initialized;

        protected ConcurrentMap<Integer, Boolean> claimed;
        protected ConcurrentMap<Integer, Boolean> claimedSpecial;
        protected ConcurrentMap<Integer, Integer> kills;

        @SuppressWarnings("all")
        protected void initialize() {
            ConcurrentMap<Integer, Boolean> claimed = Concurrent.newMap();
            ConcurrentMap<Integer, Boolean> claimedSpecial = Concurrent.newMap();
            ConcurrentMap<Integer, Integer> kills = Concurrent.newMap();

            for (Map.Entry<String, Boolean> entry : this.claimed_levels.entrySet()) {
                String entryKey = entry.getKey().replace("level_", "");
                boolean special = entryKey.endsWith("_special");
                entryKey = special ? entryKey.replace("_special", "") : entryKey;
                (special ? claimedSpecial : claimed).put(Integer.parseInt(entryKey), entry.getValue());
            }

            for (int i = 0; i < 5; i++)
                kills.put(i + 1, (int) slayerRef.getValue(String.format("boss_kills_tier_%s", i), this));

            this.claimed = claimed.toUnmodifiableMap();
            this.claimedSpecial = claimedSpecial.toUnmodifiableMap();
            this.kills = kills.toUnmodifiableMap();
            this.initialized = true;
        }

        /**
         * Wraps this class in a {@link Experience} and {@link Weight} class.
         * <br><br>
         * Requires an active database session.
         */
        public @NotNull EnhancedBoss asEnhanced() {
            return new EnhancedBoss(this);
        }

        public @NotNull ConcurrentMap<Integer, Boolean> getClaimed() {
            if (!this.initialized)
                this.initialize();

            return this.claimed;
        }

        public @NotNull ConcurrentMap<Integer, Boolean> getClaimedSpecial() {
            if (!this.initialized)
                this.initialize();

            return this.claimedSpecial;
        }

        public @NotNull ConcurrentMap<Integer, Integer> getKills() {
            if (!this.initialized)
                this.initialize();

            return this.kills;
        }

        public boolean isClaimed(int level) {
            return this.getClaimed().getOrDefault(level, false);
        }

    }

    @Getter
    public static class EnhancedBoss extends Boss implements Experience, Weighted {

        private final @NotNull SlayerModel typeModel;
        private final @NotNull ConcurrentList<Double> experienceTiers;

        private EnhancedBoss(@NotNull Boss boss) {
            // Re-initialize Fields
            this.type = boss.getType();
            this.experience = boss.getExperience();
            this.claimed_levels = boss.claimed_levels;
            this.boss_kills_tier_0 = boss.boss_kills_tier_0;
            this.boss_kills_tier_1 = boss.boss_kills_tier_1;
            this.boss_kills_tier_2 = boss.boss_kills_tier_2;
            this.boss_kills_tier_3 = boss.boss_kills_tier_3;
            this.boss_kills_tier_4 = boss.boss_kills_tier_4;
            super.initialize();

            this.typeModel = this.getType().getModel();
            this.experienceTiers = SimplifiedApi.getRepositoryOf(SlayerLevelModel.class)
                .stream()
                .filter(slayerLevel -> slayerLevel.getSlayer().getKey().equals(this.getType().name()))
                .map(SlayerLevelModel::getTotalExpRequired)
                .collect(Concurrent.toList());
        }

        @Override
        public int getMaxLevel() {
            return this.getExperienceTiers().size();
        }

        @Override
        public @NotNull Weight getWeight() {
            if (this.getTypeModel().getWeightDivider() == 0.00)
                return Weight.of(0, 0);

            ConcurrentList<Double> experienceTiers = this.getExperienceTiers();
            double maxSlayerExperienceRequired = experienceTiers.get(experienceTiers.size() - 1);
            double base = Math.min(this.getExperience(), maxSlayerExperienceRequired) / this.getTypeModel().getWeightDivider();
            double weightValue = NumberUtil.round(base, 2);
            double weightOverflow = 0;

            if (this.getExperience() > maxSlayerExperienceRequired) {
                double remaining = this.getExperience() - maxSlayerExperienceRequired;
                double overflow = 0;
                double modifier = this.getTypeModel().getWeightModifier();

                while (remaining > 0) {
                    double left = Math.min(remaining, maxSlayerExperienceRequired);
                    overflow += Math.pow(left / (this.getTypeModel().getWeightDivider() * (1.5 + modifier)), 0.942);
                    remaining -= left;
                    modifier += modifier;
                }

                weightOverflow = NumberUtil.round(overflow, 2);
            }

            return Weight.of(weightValue, weightOverflow);
        }

    }

}
