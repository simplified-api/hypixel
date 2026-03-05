package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.dungeon;

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
public class DungeonClass implements Experience, Weighted {

    private final double experience;

    @Override
    public @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return DungeonEntry.DEFAULT_TIERS;
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

    @RequiredArgsConstructor
    public enum Type {

        UNKNOWN,
        HEALER,
        MAGE,
        BERSERK,
        ARCHER,
        TANK;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name());
        }

        public static @NotNull Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

}
