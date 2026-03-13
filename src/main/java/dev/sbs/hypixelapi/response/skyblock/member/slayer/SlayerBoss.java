package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.slayer;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.reflection.Reflection;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.skyblock.common.Experience;
import dev.sbs.minecraftapi.skyblock.common.Weight;
import dev.sbs.minecraftapi.skyblock.common.Weighted;
import dev.sbs.minecraftapi.skyblock.model.Slayer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
public class SlayerBoss implements PostInit, Experience, Weighted {

    @Getter(AccessLevel.NONE)
    private final static Reflection<SlayerBoss> slayerRef = new Reflection<>(SlayerBoss.class);
    @Setter(AccessLevel.PACKAGE)
    private String id;
    @SerializedName("xp")
    private double experience;

    private ConcurrentMap<String, Boolean> claimed_levels = Concurrent.newMap(); // level_#: true
    private int boss_kills_tier_0;
    private int boss_kills_tier_1;
    private int boss_kills_tier_2;
    private int boss_kills_tier_3;
    private int boss_kills_tier_4;

    private int boss_attempts_tier_0;
    private int boss_attempts_tier_1;
    private int boss_attempts_tier_2;
    private int boss_attempts_tier_3;
    private int boss_attempts_tier_4;

    @Getter private ConcurrentMap<Integer, Boolean> claimed;
    @Getter private ConcurrentMap<Integer, Boolean> claimedSpecial;
    @Getter private ConcurrentMap<Integer, Integer> kills;
    @Getter private ConcurrentMap<Integer, Integer> attempts;

    @Override
    @SuppressWarnings("all")
    public void postInit() {
        ConcurrentMap<Integer, Boolean> claimed = Concurrent.newMap();
        ConcurrentMap<Integer, Boolean> claimedSpecial = Concurrent.newMap();
        ConcurrentMap<Integer, Integer> kills = Concurrent.newMap();
        ConcurrentMap<Integer, Integer> attempts = Concurrent.newMap();

        for (Map.Entry<String, Boolean> entry : this.claimed_levels.entrySet()) {
            String entryKey = entry.getKey().replace("level_", "");
            boolean special = entryKey.endsWith("_special");
            entryKey = special ? entryKey.replace("_special", "") : entryKey;
            (special ? claimedSpecial : claimed).put(Integer.parseInt(entryKey), entry.getValue());
        }

        for (int i = 0; i < 5; i++) {
            kills.put(i + 1, (int) slayerRef.getValue(String.format("boss_kills_tier_%s", i), this));
            attempts.put(i + 1, (int) slayerRef.getValue(String.format("boss_attempts_tier_%s", i), this));
        }

        this.claimed = claimed.toUnmodifiableMap();
        this.claimedSpecial = claimedSpecial.toUnmodifiableMap();
        this.kills = kills.toUnmodifiableMap();
        this.attempts = attempts.toUnmodifiableMap();
    }


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
        return this.getClaimed().getOrDefault(level, false);
    }


}
