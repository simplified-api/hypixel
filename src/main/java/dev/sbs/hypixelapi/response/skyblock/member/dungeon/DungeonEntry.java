package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.dungeon;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.skyblock.common.Experience;
import dev.sbs.minecraftapi.skyblock.common.Weight;
import dev.sbs.minecraftapi.skyblock.common.Weighted;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public class DungeonEntry implements Experience, Weighted {

    private final double experience;
    private final @NotNull FloorData normalMode;
    private final @NotNull FloorData masterMode;

    @Override
    public @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return DEFAULT_TIERS;
    }

    public @NotNull FloorData getFloorData(boolean masterMode) {
        return masterMode ? this.getMasterMode() : this.getNormalMode();
    }

    @Override
    public int getMaxLevel() {
        return this.getExperienceTiers().size();
    }

    @Override
    public @NotNull Weight getWeight() {
        double rawLevel = this.getRawLevel();
        ConcurrentList<Integer> experienceTiers = this.getExperienceTiers();
        double maxDungeonClassExperienceRequired = experienceTiers.get(experienceTiers.size() - 1);

        if (rawLevel < this.getMaxLevel())
            rawLevel += (this.getProgressPercentage() / 100); // Add Percentage Progress to Next Level

        double base = Math.pow(rawLevel, 4.5) * 0.0000045254834; // Weight Multiplier
        double weightValue = NumberUtil.round(base, 2);
        double weightOverflow = 0;

        if (this.getExperience() > maxDungeonClassExperienceRequired) {
            double overflow = Math.pow((this.getExperience() - maxDungeonClassExperienceRequired) / (4 * maxDungeonClassExperienceRequired / base), 0.968);
            weightOverflow = NumberUtil.round(overflow, 2);
        }

        return Weight.of(weightValue, weightOverflow);
    }

    @Getter
    @RequiredArgsConstructor
    public enum Type {

        UNKNOWN,
        CATACOMBS;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

        public static @NotNull Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

    static final @NotNull ConcurrentList<Integer> DEFAULT_TIERS = Concurrent.newUnmodifiableList(
        50, 125, 235, 395, 625, 955, 1_425, 2_095, 3_045, 4_385,
        6_275, 8_940, 12_700, 17_960, 25_340, 35_640, 50_040, 70_040, 97_640, 135_640,
        188_140, 259_640, 356_640, 488_640, 668_640, 911_640, 1_239_640, 1_684_640, 2_284,640, 3_084_640,
        4_149_640, 5_559_640, 7_459_640, 9_959_640, 13_259_640, 17_559_640, 23_159_640, 30_359_640, 39_559_640, 51_559_640,
        66_559_640, 85_559_640, 109_559_640, 139_559_640, 177_559_640, 225_559_640, 285_559_640, 360_559_640, 453_559_640, 569_559_640
    );

}
