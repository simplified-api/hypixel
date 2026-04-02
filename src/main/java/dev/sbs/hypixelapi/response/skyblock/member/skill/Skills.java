package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.skill;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockMember;
import dev.sbs.minecraftapi.skyblock.common.Weight;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class Skills {

    private final @NotNull ConcurrentList<SkillLevel> skillLevels;

    public Skills(@NotNull ConcurrentMap<String, Double> skillExperience, @NotNull SkyBlockMember member) {
        this.skillLevels = skillExperience.stream()
            .mapKey(id -> id.replace("SKILL_", ""))
            .collapseToSingle((id, experience) -> new SkillLevel(id, experience, member))
            .collect(Concurrent.toUnmodifiableList());
    }

    public @NotNull SkillLevel getSkill(@NotNull String id) {
        return this.getSkillLevels().matchFirstOrNull(skill -> skill.getId().equalsIgnoreCase(id));
    }

    public @NotNull ConcurrentList<SkillLevel> getSkillLevels(boolean includeCosmetic) {
        return this.getSkillLevels()
            .stream()
            .filter(skill -> includeCosmetic || !skill.getSkill().isCosmetic())
            .collect(Concurrent.toList());
    }

    public double getAverage() {
        return this.getSkillLevels(false)
            .stream()
            .mapToDouble(SkillLevel::getLevel)
            .average()
            .orElse(0.0);
    }

    public double getExperience() {
        return this.getSkillLevels(false)
            .stream()
            .mapToDouble(SkillLevel::getExperience)
            .sum();
    }

    public double getProgressPercentage() {
        return this.getSkillLevels(false)
            .stream()
            .mapToDouble(SkillLevel::getTotalProgressPercentage)
            .average()
            .orElse(0.0);
    }

    public @NotNull ConcurrentMap<SkillLevel, Weight> getWeight() {
        return this.getSkillLevels(false)
            .stream()
            .map(skill -> Pair.of(skill, skill.getWeight()))
            .collect(Concurrent.toMap());
    }

}
