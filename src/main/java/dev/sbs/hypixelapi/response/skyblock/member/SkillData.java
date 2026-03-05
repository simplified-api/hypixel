package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockMember;
import dev.sbs.minecraftapi.skyblock.common.Experience;
import dev.sbs.minecraftapi.skyblock.common.Weight;
import dev.sbs.minecraftapi.skyblock.common.Weighted;
import dev.sbs.minecraftapi.skyblock.model.Skill;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class SkillData {

    private final @NotNull ConcurrentList<Entry> skills;

    public SkillData(@NotNull ConcurrentMap<String, Double> skillExperience, @NotNull SkyBlockMember member) {
        this.skills = skillExperience.stream()
            .mapKey(id -> id.replace("SKILL_", ""))
            .mapToObj((id, experience) -> new Entry(id, experience, member))
            .collect(Concurrent.toUnmodifiableList());
    }

    public @NotNull Entry getSkill(@NotNull String id) {
        return this.getSkills().matchFirstOrNull(skill -> skill.getId().equalsIgnoreCase(id));
    }

    public @NotNull ConcurrentList<Entry> getSkills(boolean includeCosmetic) {
        return this.getSkills()
            .stream()
            .filter(skill -> includeCosmetic || !skill.getSkill().isCosmetic())
            .collect(Concurrent.toList());
    }

    public double getAverage() {
        return this.getSkills(false)
            .stream()
            .mapToDouble(Entry::getLevel)
            .average()
            .orElse(0.0);
    }

    public double getExperience() {
        return this.getSkills(false)
            .stream()
            .mapToDouble(Entry::getExperience)
            .sum();
    }

    public double getProgressPercentage() {
        return this.getSkills(false)
            .stream()
            .mapToDouble(Entry::getTotalProgressPercentage)
            .average()
            .orElse(0.0);
    }

    public @NotNull ConcurrentMap<Entry, Weight> getWeight() {
        return this.getSkills(false)
            .stream()
            .map(skill -> Pair.of(skill, skill.getWeight()))
            .collect(Concurrent.toMap());
    }

    @Getter
    public static class Entry implements Experience, Weighted {

        private final @NotNull String id;
        private final double experience;
        private final int levelSubtractor;

        private Entry(@NotNull String id, double experience, @NotNull SkyBlockMember member) {
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
                    // TODO: Hopefully collection unlocks are in the api
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

}
