package api.simplified.hypixel.response.skyblock.member.skill;

import api.simplified.hypixel.common.WeightedGroup;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class Skills implements WeightedGroup<SkillLevel> {

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

    /** {@inheritDoc} */
    @Override
    public @NotNull ConcurrentList<SkillLevel> getWeighted() {
        return this.getSkillLevels(false);
    }

}
