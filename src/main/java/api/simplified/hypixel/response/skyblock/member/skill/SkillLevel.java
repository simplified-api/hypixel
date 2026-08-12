package api.simplified.hypixel.response.skyblock.member.skill;

import api.simplified.hypixel.common.Experience;
import api.simplified.hypixel.common.Weight;
import api.simplified.hypixel.common.Weighted;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Skill;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.util.NumberUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * One skill's experience total for a member, with the level and weight derived from it.
 *
 * <p>
 * Nothing here is bound - there is no {@code skills} node on the wire. A {@link Skills} group builds
 * these from {@code player_data.experience}, one per {@code SKILL_} key. Dungeoneering is not among
 * them; it rides in the {@code dungeons} node instead.
 *
 * <p>
 * {@link #getSkill()} is the repository boundary for the whole class - the experience tiers, the
 * maximum level and the weight are all resolved through it and need a session.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Skills">Skills</a>
 */
@Getter
public class SkillLevel implements Experience, Weighted {

    /**
     * Skill id, taken from the {@code player_data.experience} map key with its {@code SKILL_} prefix
     * stripped off.
     */
    private final @NotNull String id;

    /**
     * Experience earned in this skill, clamped to zero when the wire carried a negative total.
     */
    private final double experience;

    /**
     * How many levels short of the repository maximum this member's cap for the skill sits.
     */
    private final int levelSubtractor;

    /**
     * Constructs a skill level from one entry of a member's skill experience.
     *
     * <p>
     * Member state is read here rather than on demand, so the cap in force when the level was built
     * is the cap it reports.
     *
     * @param id the skill id, without its {@code SKILL_} prefix
     * @param experience experience earned in this skill, clamped to zero when negative
     * @param member the member the skill belongs to, read for its farming cap and unlocked collections
     */
    public SkillLevel(@NotNull String id, double experience, @NotNull SkyBlockMember member) {
        this.id = id;
        this.experience = Math.max(experience, 0);
        this.levelSubtractor = this.calcLevelSubtractor(member);
    }

    /**
     * Computes how many levels below the repository maximum this member's cap for the skill sits.
     *
     * <p>
     * Farming is capped by the gold medals bought against Jacob's contests, one crop at a time;
     * foraging loses a level for each log collection that is unlocked but not yet maxed. Every other
     * skill is uncapped.
     *
     * @param member the member whose contest perks and unlocked collections set the cap
     * @return the number of levels to subtract from the skill's maximum
     */
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

    /**
     * Repository row backing this skill - its experience tiers, maximum level and weight curve.
     *
     * <p>
     * Resolved, so it needs a session. An id with no matching row resolves to null despite the
     * annotation, and every accessor reached through it fails on that null.
     */
    public @NotNull Skill getSkill() {
        return SkyBlockData.getRepository(Skill.class).findFirstOrNull(Skill::getId, this.getId());
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return this.getSkill().getExperienceTiers();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxLevel() {
        return this.getSkill().getMaxLevel();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull Weight getWeight() {
        if (this.getSkill().getWeightDivider() == 0.0)
            return Weight.of(0, 0);

        double rawLevel = this.getRawLevel();
        ConcurrentList<Integer> experienceTiers = this.getExperienceTiers();
        double maxSkillExperienceRequired = experienceTiers.getLast();

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
