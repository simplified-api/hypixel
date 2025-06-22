package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.skill;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.JacobsContest;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.Experience;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.weight.Weight;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.weight.Weighted;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.data.model.skill_levels.SkillLevelModel;
import dev.sbs.minecraftapi.data.model.skills.SkillModel;
import dev.sbs.api.util.NumberUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class EnhancedSkill extends Skill implements Experience, Weighted {

    private final @NotNull SkillModel typeModel;
    private final int levelSubtractor;
    private final @NotNull ConcurrentList<Double> experienceTiers;

    EnhancedSkill(@NotNull Skill skill, @NotNull JacobsContest jacobsContest) {
        super(skill.getType(), skill.getExperience());
        this.typeModel = this.getType().getModel();
        this.levelSubtractor = this.getType() == Type.FARMING ? 10 - jacobsContest.getFarmingLevelCap() : 0;

        this.experienceTiers = SimplifiedApi.getRepositoryOf(SkillLevelModel.class)
            .stream()
            .filter(slayerLevel -> slayerLevel.getSkill().getKey().equals(this.getType().name()))
            .map(SkillLevelModel::getTotalExpRequired)
            .collect(Concurrent.toList());
    }

    @Override
    public double getExperience() {
        return Math.max(this.experience, 0);
    }

    @Override
    public int getMaxLevel() {
        return this.getTypeModel().getMaxLevel();
    }

    @Override
    public @NotNull Weight getWeight() {
        if (this.getTypeModel().getWeightDivider() == 0.00)
            return Weight.of(0, 0);

        double rawLevel = this.getRawLevel();
        ConcurrentList<Double> experienceTiers = this.getExperienceTiers();
        double maxSkillExperienceRequired = experienceTiers.get(experienceTiers.size() - 1);

        if (rawLevel < this.getMaxLevel())
            rawLevel += (this.getProgressPercentage() / 100); // Add Percentage Progress to Next Level

        double base = Math.pow(rawLevel * 10, 0.5 + this.getTypeModel().getWeightExponent() + (rawLevel / 100.0)) / 1250;
        double weightValue = NumberUtil.round(base, 2);
        double weightOverflow = 0;

        if (this.getExperience() > maxSkillExperienceRequired) {
            double overflow = Math.pow((this.getExperience() - maxSkillExperienceRequired) / this.getTypeModel().getWeightDivider(), 0.968);
            weightOverflow = NumberUtil.round(overflow, 2);
        }

        return Weight.of(weightValue, weightOverflow);
    }

}
