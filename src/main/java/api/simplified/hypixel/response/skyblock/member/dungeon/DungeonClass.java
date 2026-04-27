package api.simplified.hypixel.response.skyblock.member.dungeon;

import api.simplified.hypixel.common.Experience;
import api.simplified.hypixel.common.Weight;
import api.simplified.hypixel.common.Weighted;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.util.NumberUtil;
import dev.simplified.util.StringUtil;
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
        return DungeonData.DEFAULT_TIERS;
    }

    @Override
    public int getMaxLevel() {
        return this.getExperienceTiers().size();
    }

    @Override
    public @NotNull Weight getWeight() {
        double rawLevel = this.getRawLevel();
        ConcurrentList<Integer> experienceTiers = this.getExperienceTiers();
        double maxDungeonClassExperienceRequired = experienceTiers.getLast();

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
