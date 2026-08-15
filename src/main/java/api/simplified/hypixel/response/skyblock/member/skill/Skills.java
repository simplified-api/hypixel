package api.simplified.hypixel.response.skyblock.member.skill;

import api.simplified.hypixel.common.WeightedGroup;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.RequiredArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

/**
 * A member's skill progressions as one group, and the aggregates folded over them.
 *
 * <p>
 * Nothing here is bound - there is no {@code skills} node on the wire. The group is built from
 * {@code player_data.experience} and memoised on the member, so the accessor that hands it back is
 * the only thing that computes it.
 *
 * <p>
 * The aggregates inherited from {@link WeightedGroup} run over {@link #getWeighted()}, which drops
 * the cosmetic skills. Which skills those are is repository data rather than a list written here.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Skills">Skills</a>
 */
@Getter
@RequiredArgsConstructor
public class Skills implements WeightedGroup<SkillLevel> {

    /**
     * One progression per skill the member has experience in.
     */
    private final @NotNull ConcurrentList<SkillLevel> skillLevels;

    /**
     * Constructs the group from a member's raw skill experience.
     *
     * @param skillExperience experience per skill, keyed by the wire's {@code SKILL_}-prefixed ids
     * @param member the member the skills belong to, read for each skill's level cap
     */
    public Skills(@NotNull ConcurrentMap<String, Double> skillExperience, @NotNull SkyBlockMember member) {
        this.skillLevels = skillExperience.stream()
            .mapKey(id -> id.replace("SKILL_", ""))
            .collapseToSingle((id, experience) -> new SkillLevel(id, experience, member))
            .collect(Concurrent.toUnmodifiableList());
    }

    /**
     * Finds one skill progression by id, ignoring case.
     *
     * <p>
     * This hands back the member's progression, not the repository row describing the skill - that
     * is {@link SkillLevel#getSkill()}, one call further on.
     *
     * @param id the skill id, without its {@code SKILL_} prefix
     * @return the matching progression, or null when no skill carries that id
     */
    public @NotNull SkillLevel getSkill(@NotNull String id) {
        return this.getSkillLevels().matchFirstOrNull(skill -> skill.getId().equalsIgnoreCase(id));
    }

    /**
     * Selects the skill progressions that count toward the group's aggregates.
     *
     * <p>
     * Cosmetic is a flag on the repository row, so this reaches the skill repository and needs a
     * session.
     *
     * @param includeCosmetic whether to keep the skills flagged cosmetic
     * @return the progressions to fold over
     */
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
