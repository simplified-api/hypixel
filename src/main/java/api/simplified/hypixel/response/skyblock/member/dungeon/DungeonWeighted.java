package api.simplified.hypixel.response.skyblock.member.dungeon;

import api.simplified.hypixel.common.Experience;
import api.simplified.hypixel.common.Weight;
import api.simplified.hypixel.common.Weighted;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.util.NumberUtil;
import org.jetbrains.annotations.NotNull;

/**
 * The progression a dungeon and a dungeon class share.
 * <p>
 * Both climb the same fifty-step experience table and both turn their level into weight the same
 * way, so the only thing that differs between them is which experience total they read. Holding one
 * copy of the formula is what keeps the two from drifting apart, since
 * {@link SkyBlockMember#getTotalWeight()} sums the dungeon weight and the class weights into a
 * single number that would not show the difference.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Dungeoneering">Dungeoneering</a>
 */
interface DungeonWeighted extends Experience, Weighted {

    /**
     * The fifty running experience totals needed to reach each level, {@code 50} for level one up to
     * {@code 569_559_640} for level fifty.
     * <p>
     * Their sum, not the level-fifty threshold, is what lifetime progress is measured against, so
     * those percentages come out small.
     */
    @NotNull ConcurrentList<Integer> DEFAULT_TIERS = Concurrent.newUnmodifiableList(
        50, 125, 235, 395, 625, 955, 1_425, 2_095, 3_045, 4_385,
        6_275, 8_940, 12_700, 17_960, 25_340, 35_640, 50_040, 70_040, 97_640, 135_640,
        188_140, 259_640, 356_640, 488_640, 668_640, 911_640, 1_239_640, 1_684_640, 2_284_640, 3_084_640,
        4_149_640, 5_559_640, 7_459_640, 9_959_640, 13_259_640, 17_559_640, 23_159_640, 30_359_640, 39_559_640, 51_559_640,
        66_559_640, 85_559_640, 109_559_640, 139_559_640, 177_559_640, 225_559_640, 285_559_640, 360_559_640, 453_559_640, 569_559_640
    );

    /** {@inheritDoc} */
    @Override
    default @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return DEFAULT_TIERS;
    }

    /** {@inheritDoc} */
    @Override
    default int getMaxLevel() {
        return this.getExperienceTiers().size();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The level is raised to a power and scaled, with the fractional progress towards the next level
     * folded into the base while still below the maximum. Experience past the level-fifty threshold
     * is scored separately as overflow rather than being added to the value.
     */
    @Override
    default @NotNull Weight getWeight() {
        double rawLevel = this.getRawLevel();
        double maxExperienceRequired = this.getExperienceTiers().getLast();

        if (rawLevel < this.getMaxLevel())
            rawLevel += (this.getProgressPercentage() / 100); // Add Percentage Progress to Next Level

        double base = Math.pow(rawLevel, 4.5) * 0.0000045254834; // Weight Multiplier
        double weightValue = NumberUtil.round(base, 2);
        double weightOverflow = 0;

        if (this.getExperience() > maxExperienceRequired) {
            double overflow = Math.pow((this.getExperience() - maxExperienceRequired) / (4 * maxExperienceRequired / base), 0.968);
            weightOverflow = NumberUtil.round(overflow, 2);
        }

        return Weight.of(weightValue, weightOverflow);
    }

}
