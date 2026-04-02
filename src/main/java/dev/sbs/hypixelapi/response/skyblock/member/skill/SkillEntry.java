package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.skill;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockMember;
import dev.sbs.minecraftapi.persistence.model.Skill;
import dev.sbs.minecraftapi.skyblock.common.Experience;
import dev.sbs.minecraftapi.skyblock.common.Weight;
import dev.sbs.minecraftapi.skyblock.common.Weighted;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class SkillEntry implements Experience, Weighted {

    private final @NotNull String id;
    private final double experience;
    private final int levelSubtractor;

    public SkillEntry(@NotNull String id, double experience, @NotNull SkyBlockMember member) {
        this.id = id;
        this.experience = Math.max(experience, 0);
        this.levelSubtractor = this.calcLevelSubtractor(member);
    }

    private int calcLevelSubtractor(@NotNull SkyBlockMember member) {
        return switch (this.getId()) {
            case "FARMING" -> 10 - member.getJacobsContest().getFarmingLevelCap();
            case "FORAGING" -> {
                int subtractor = 0;
                subtractor += member.getCollectionUnlocked().getOrDefault("FIG_LOG", 0) < 9 ? 1 : 0;
                subtractor += member.getCollectionUnlocked().getOrDefault("MANGROVE_LOG", 0) < 9 ? 1 : 0;
                // TODO: Agatha Shop
                //       Hopefully collection unlocks are in the api
                yield subtractor;
            }
            default -> 0;
        };
    }

    public @NotNull Skill getSkill() {
        return MinecraftApi.getRepository(Skill.class).findFirstOrNull(Skill::getId, this.getId());
    }

    @Override
    public @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return this.getSkill().getExperienceTiers();
    }

    @Override
    public int getMaxLevel() {
        return this.getSkill().getMaxLevel();
    }

    @Override
    public @NotNull Weight getWeight() {
        if (this.getSkill().getWeightDivider() == 0.0)
            return Weight.of(0, 0);

        double rawLevel = this.getRawLevel();
        ConcurrentList<Integer> experienceTiers = this.getExperienceTiers();
        double maxSkillExperienceRequired = experienceTiers.get(experienceTiers.size() - 1);

        if (rawLevel < this.getMaxLevel())
            rawLevel += (this.getProgressPercentage() / 100); // Add Percentage Progress to Next Level

        double base = Math.pow(rawLevel * 10, 0.5 + this.getSkill().getWeightExponent() + (rawLevel / 100.0)) / 1250;
        double weightValue = NumberUtil.round(base, 2);
        double weightOverflow = 0;

        if (this.getExperience() > maxSkillExperienceRequired) {
            double overflow = Math.pow((this.getExperience() - maxSkillExperienceRequired) / this.getSkill().getWeightDivider(), 0.968);
            weightOverflow = NumberUtil.round(overflow, 2);
        }

        return Weight.of(weightValue, weightOverflow);
    }

}
