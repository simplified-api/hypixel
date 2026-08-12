package api.simplified.hypixel.common;

import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

/**
 * A progression that turns an experience total into a level against a table of tiers.
 * <p>
 * Every tier is the running total needed to reach that level, not the step from the one below, so a
 * level is simply the count of tiers the experience has passed. Implementations supply the table and
 * the cap; everything else here is derived from those two and never touches the wire or a repository.
 */
public interface Experience {

    /**
     * Level a progression sits at before any experience is earned, zero for most and one for a pet.
     */
    default int getStartingLevel() {
        return 0;
    }

    /**
     * Experience earned so far.
     */
    double getExperience();

    /**
     * Running experience total needed to reach each level, lowest first.
     */
    @NotNull ConcurrentList<Integer> getExperienceTiers();

    /**
     * Levels trimmed off the top of the table, for a progression whose cap sits below the tiers it
     * carries.
     */
    default int getLevelSubtractor() {
        return 0;
    }

    /**
     * Level reached, capped at the maximum.
     */
    default int getLevel() {
        return this.getLevel(this.getExperience());
    }

    /**
     * Reads the capped level a given experience total would reach.
     *
     * @param experience the experience total to measure
     * @return the level, never above the maximum less the subtractor
     */
    default int getLevel(double experience) {
        return Math.min(this.getRawLevel(experience), this.getMaxLevel() - this.getLevelSubtractor());
    }

    /**
     * Level reached ignoring the cap, so it may exceed the maximum.
     */
    default int getRawLevel() {
        return this.getRawLevel(this.getExperience());
    }

    /**
     * Counts the tiers a given experience total has passed, ignoring the cap.
     *
     * @param experience the experience total to measure
     * @return the index of the first tier the experience has not reached, or the maximum when it has
     * passed them all
     */
    default int getRawLevel(double experience) {
        ConcurrentList<Integer> experienceTiers = this.getExperienceTiers();

        return IntStream.range(0, experienceTiers.size())
            .filter(index -> experienceTiers.get(index) > experience)
            .findFirst()
            .orElseGet(this::getMaxLevel);
    }

    /**
     * Highest level this progression can reach.
     */
    int getMaxLevel();

    /**
     * Experience the current level spans, zero once the maximum is reached.
     */
    default double getNextExperience() {
        return this.getNextExperience(this.getExperience());
    }

    /**
     * Measures the experience between the level a total sits in and the one above it.
     *
     * @param experience the experience total to measure
     * @return the span of the current level, the first tier while still below level one, or zero at
     * the maximum
     */
    default double getNextExperience(double experience) {
        ConcurrentList<Integer> experienceTiers = this.getExperienceTiers();
        int rawLevel = this.getRawLevel(experience);

        if (rawLevel == 0)
            return experienceTiers.getFirst();
        else if (rawLevel >= this.getMaxLevel())
            return 0;
        else
            return experienceTiers.get(rawLevel) - experienceTiers.get(rawLevel - 1);
    }

    /**
     * Experience earned into the current level, with the levels already passed taken off.
     */
    default double getProgressExperience() {
        return this.getProgressExperience(this.getExperience());
    }

    /**
     * Measures how far a total has come into the level it sits in.
     *
     * @param experience the experience total to measure
     * @return the experience past the level's opening tier, or the whole total while still below
     * level one
     */
    default double getProgressExperience(double experience) {
        ConcurrentList<Integer> experienceTiers = this.getExperienceTiers();
        int rawLevel = this.getRawLevel(experience);

        try {
            if (rawLevel == 0)
                return experience;
            else if (rawLevel >= this.getMaxLevel())
                return experience - experienceTiers.getLast();
        } catch (Exception ignore) { }

        return experience - experienceTiers.get(rawLevel - 1);
    }

    /**
     * Experience still owed before the next level, zero once the maximum is reached.
     */
    default double getMissingExperience() {
        return this.getMissingExperience(this.getExperience());
    }

    /**
     * Measures the experience a total still owes before the level above it.
     *
     * @param experience the experience total to measure
     * @return the shortfall to the next tier, or zero at the maximum
     */
    default double getMissingExperience(double experience) {
        ConcurrentList<Integer> experienceTiers = this.getExperienceTiers();
        int rawLevel = this.getRawLevel(experience);
        return rawLevel >= this.getMaxLevel() ? 0 : (experienceTiers.get(rawLevel) - experience);
    }

    /**
     * Progress through the current level as a percentage, a full hundred at the maximum.
     */
    default double getProgressPercentage() {
        return this.getProgressPercentage(this.getExperience());
    }

    /**
     * Measures progress through the level a total sits in.
     *
     * @param experience the experience total to measure
     * @return the percentage into the current level, or a full hundred once the level spans nothing
     * further
     */
    default double getProgressPercentage(double experience) {
        double progressExperience = this.getProgressExperience(experience);
        double nextExperience = this.getNextExperience(experience);
        return nextExperience == 0 ? 100.0 : (progressExperience / nextExperience) * 100.0;
    }

    /**
     * Sum of every tier in the table, used as the denominator for lifetime progress.
     */
    default double getTotalExperience() {
        return this.getExperienceTiers()
            .stream()
            .mapToDouble(Integer::doubleValue)
            .sum();
    }

    /**
     * Lifetime progress as a percentage, measured against the summed table rather than one level.
     */
    default double getTotalProgressPercentage() {
        return (this.getExperience() / this.getTotalExperience()) * 100.0;
    }

}
