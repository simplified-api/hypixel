package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.slayer;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.persistence.model.Slayer;
import dev.sbs.minecraftapi.skyblock.common.Experience;
import dev.sbs.minecraftapi.skyblock.common.Weight;
import dev.sbs.minecraftapi.skyblock.common.Weighted;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.Capture;
import dev.simplified.gson.Key;
import dev.simplified.util.NumberUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
public class SlayerBoss implements Experience, Weighted {

    @Key
    private transient String id;
    @SerializedName("xp")
    private double experience;
    @Capture(filter = "^level_", descend = true)
    @SerializedName("claimed_levels")
    private @NotNull ConcurrentMap<Integer, ClaimedLevel> claimedLevels = Concurrent.newMap();
    @Capture(filter = "^boss_kills_tier_")
    private @NotNull ConcurrentMap<Integer, Integer> kills = Concurrent.newMap();
    @Capture(filter = "^boss_attempts_tier_")
    private @NotNull ConcurrentMap<Integer, Integer> attempts = Concurrent.newMap();

    public @NotNull Slayer getSlayer() {
        return MinecraftApi.getRepository(Slayer.class).findFirstOrNull(Slayer::getId, this.getId());
    }

    @Override
    public @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return this.getSlayer().getExperienceTiers();
    }

    @Override
    public int getMaxLevel() {
        return this.getSlayer().getMaxLevel();
    }

    @Override
    public @NotNull Weight getWeight() {
        if (this.getSlayer().getWeightDivider() == 0.0)
            return Weight.of(0, 0);

        ConcurrentList<Integer> experienceTiers = this.getExperienceTiers();
        double maxSlayerExperienceRequired = experienceTiers.get(experienceTiers.size() - 1);
        double base = Math.min(this.getExperience(), maxSlayerExperienceRequired) / this.getSlayer().getWeightDivider();
        double weightValue = NumberUtil.round(base, 2);
        double weightOverflow = 0;

        if (this.getExperience() > maxSlayerExperienceRequired) {
            double remaining = this.getExperience() - maxSlayerExperienceRequired;
            double overflow = 0;
            double modifier = this.getSlayer().getWeightModifier();

            while (remaining > 0) {
                double left = Math.min(remaining, maxSlayerExperienceRequired);
                overflow += Math.pow(left / (this.getSlayer().getWeightDivider() * (1.5 + modifier)), 0.942);
                remaining -= left;
                modifier += modifier;
            }

            weightOverflow = NumberUtil.round(overflow, 2);
        }

        return Weight.of(weightValue, weightOverflow);
    }

    public boolean isClaimed(int level) {
        ClaimedLevel data = this.getClaimedLevels().get(level);
        return data != null && data.isClaimed();
    }

    @Getter
    @NoArgsConstructor
    public static class ClaimedLevel {

        @SerializedName("")
        private boolean claimed;
        private boolean special;

    }

}
