package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

public interface Experience {

    default int getStartingLevel() {
        return 0;
    }

    double getExperience();

    @NotNull ConcurrentList<Double> getExperienceTiers();

    default int getLevelSubtractor() {
        return 0;
    }

    default int getLevel() {
        return this.getLevel(this.getExperience());
    }

    default int getLevel(double experience) {
        return Math.min(this.getRawLevel(experience), this.getMaxLevel() - this.getLevelSubtractor());
    }

    default int getRawLevel() {
        return this.getRawLevel(this.getExperience());
    }

    default int getRawLevel(double experience) {
        ConcurrentList<Double> experienceTiers = this.getExperienceTiers();

        return IntStream.range(0, experienceTiers.size())
            .filter(index -> experienceTiers.get(index) > experience)
            .findFirst()
            .orElseGet(this::getMaxLevel);
    }

    int getMaxLevel();

    default double getNextExperience() {
        return this.getNextExperience(this.getExperience());
    }

    default double getNextExperience(double experience) {
        ConcurrentList<Double> experienceTiers = this.getExperienceTiers();
        int rawLevel = this.getRawLevel(experience);

        if (rawLevel == 0)
            return experienceTiers.get(0);
        else if (rawLevel >= this.getMaxLevel())
            return 0;
        else
            return experienceTiers.get(rawLevel) - experienceTiers.get(rawLevel - 1);
    }

    default double getProgressExperience() {
        return this.getProgressExperience(this.getExperience());
    }

    default double getProgressExperience(double experience) {
        ConcurrentList<Double> experienceTiers = this.getExperienceTiers();
        int rawLevel = this.getRawLevel(experience);

        try {
            if (rawLevel == 0)
                return experience;
            else if (rawLevel >= this.getMaxLevel())
                return experience - experienceTiers.get(experienceTiers.size() - 1);
        } catch (Exception ignore) { }

        return experience - experienceTiers.get(rawLevel - 1);
    }

    default double getMissingExperience() {
        return this.getMissingExperience(this.getExperience());
    }

    default double getMissingExperience(double experience) {
        ConcurrentList<Double> experienceTiers = this.getExperienceTiers();
        int rawLevel = this.getRawLevel(experience);
        return rawLevel >= this.getMaxLevel() ? 0 : (experienceTiers.get(rawLevel) - experience);
    }

    default double getProgressPercentage() {
        return this.getProgressPercentage(this.getExperience());
    }

    default double getProgressPercentage(double experience) {
        double progressExperience = this.getProgressExperience(experience);
        double nextExperience = this.getNextExperience(experience);
        return nextExperience == 0 ? 100.0 : (progressExperience / nextExperience) * 100.0;
    }

    default double getTotalExperience() {
        return this.getExperienceTiers()
            .stream()
            .mapToDouble(Double::doubleValue)
            .sum();
    }

    default double getTotalProgressPercentage() {
        return (this.getExperience() / this.getTotalExperience()) * 100.0;
    }

}
