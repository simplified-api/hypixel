package api.simplified.hypixel.response.skyblock.member.slayer;

import api.simplified.hypixel.common.Experience;
import api.simplified.hypixel.common.Weight;
import api.simplified.hypixel.common.Weighted;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Slayer;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import dev.simplified.gson.annotation.Key;
import dev.simplified.util.NumberUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * One slayer branch's lifetime record for a member - experience, the reward levels claimed, and the
 * bosses killed or attempted at each tier.
 *
 * <p>
 * The wire keys the six branches by mob rather than by boss name: {@code zombie} is Revenant Horror
 * in the Graveyard, {@code spider} is Tarantula Broodfather in the Spider's Den, {@code wolf} is
 * Sven Packmaster in the Park, {@code enderman} is Voidgloom Seraph in the End, {@code blaze} is
 * Inferno Demonlord on the Crimson Isle, and {@code vampire} is Riftstalker Bloodfiend in the Rift.
 *
 * <p>
 * A branch the member has never touched is absent from the wire entirely rather than present and
 * empty, which is why each map here defaults non-null.
 *
 * <p>
 * {@link #getSlayer()} is the repository boundary for the whole class - the experience tiers, the
 * maximum level and the weight are all resolved through it and need a session.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Slayer">Slayer</a>
 */
@Getter
public class SlayerBoss implements Experience, Weighted {

    @Getter(AccessLevel.NONE)
    @Key
    private transient @NotNull String id = "";

    /**
     * Experience earned against this branch.
     */
    @SerializedName("xp")
    private double experience;

    /**
     * Reward levels the member has collected, keyed by level number. Every key matching the level
     * filter is folded in, so a level and its {@code _special} twin arrive as one entry.
     */
    @Capture(filter = "^level_", descend = true)
    @SerializedName("claimed_levels")
    private @NotNull ConcurrentMap<Integer, ClaimedLevel> claimedLevels = Concurrent.newMap();

    /**
     * Bosses killed, keyed by tier, from every {@code boss_kills_tier_} key the wire sent. Tier zero
     * is a real key, and reaching a high tier does not imply the lower ones are present.
     */
    @Capture(filter = "^boss_kills_tier_")
    private @NotNull ConcurrentMap<Integer, Integer> kills = Concurrent.newMap();

    /**
     * Quests begun against this branch, keyed by tier, from every {@code boss_attempts_tier_} key the
     * wire sent. These are quests started rather than quests failed.
     */
    @Capture(filter = "^boss_attempts_tier_")
    private @NotNull ConcurrentMap<Integer, Integer> attempts = Concurrent.newMap();

    /**
     * Branch name, upper-cased to match the ids the reference data is keyed by - the wire spells it
     * lower-case here, as it does everywhere it names a branch.
     *
     * <p>
     * It is the key this object hung off rather than anything in its own body, so it does not
     * survive a write.
     */
    public @NotNull String getId() {
        return this.id.toUpperCase(Locale.ROOT);
    }

    /**
     * Repository row backing this branch - its experience tiers, maximum level and weight curve.
     *
     * <p>
     * Resolved, so it needs a session. A branch id with no matching row resolves to null despite the
     * annotation. The level cap lives in that row, which is why nothing here hard-codes one.
     */
    public @NotNull Slayer getSlayer() {
        return SkyBlockData.getRepository(Slayer.class).findFirstOrNull(Slayer::getId, this.getId());
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return this.getSlayer().getExperienceTiers();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxLevel() {
        return this.getSlayer().getMaxLevel();
    }

    /** {@inheritDoc} */
    @Override
    public @NotNull Weight getWeight() {
        if (this.getSlayer().getWeightDivider() == 0.0)
            return Weight.of(0, 0);

        ConcurrentList<Integer> experienceTiers = this.getExperienceTiers();
        double maxSlayerExperienceRequired = experienceTiers.getLast();
        double base = Math.min(this.getExperience(), maxSlayerExperienceRequired) / this.getSlayer().getWeightDivider();
        double weightValue = NumberUtil.round(base, 2);
        double weightOverflow = 0;

        if (this.getExperience() > maxSlayerExperienceRequired) {
            double remaining = this.getExperience() - maxSlayerExperienceRequired;
            double overflow = 0;
            double modifier = this.getSlayer().getWeightModifier();

            while (remaining > 0) {
                double left = Math.min(remaining, maxSlayerExperienceRequired);
                overflow += Math.pow(left / (this.getSlayer().getWeightDivider() * (1.5 + modifier)), 0.942);
                remaining -= left;
                modifier += modifier;
            }

            weightOverflow = NumberUtil.round(overflow, 2);
        }

        return Weight.of(weightValue, weightOverflow);
    }

    /**
     * Reports whether a reward level has been collected.
     *
     * @param level the reward level to check
     * @return {@code true} when the level is present and claimed, {@code false} when it is either
     *         unclaimed or absent
     */
    public boolean isClaimed(int level) {
        ClaimedLevel data = this.getClaimedLevels().get(level);
        return data != null && data.isClaimed();
    }

    /**
     * One reward level of a slayer branch, and whether its special variant was taken alongside it.
     */
    @Getter
    @NoArgsConstructor
    public static class ClaimedLevel {

        /**
         * Whether the reward for this level was collected, bound from the value sitting on the bare
         * level key.
         */
        @SerializedName("")
        private boolean claimed;

        /**
         * Whether the level's {@code _special} twin was collected, folded onto the same entry.
         */
        private boolean special;

    }

}
