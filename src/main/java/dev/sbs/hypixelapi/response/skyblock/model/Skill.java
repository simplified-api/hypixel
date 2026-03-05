package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.Model;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.minecraftapi.MinecraftApi;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface Skill extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getDescription();

    default @NotNull ConcurrentMap<String, Double> getEffects() {
        return this.getLevels()
            .stream()
            .flatMap(level -> level.getEffects().stream())
            .collect(Concurrent.toUnmodifiableMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Double::sum
            ));
    }

    default @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return this.getLevels()
            .stream()
            .map(Level::getTotalRequiredXP)
            .collect(Concurrent.toUnmodifiableList());
    }

    @NotNull ConcurrentList<Level> getLevels();

    int getMaxLevel();

    boolean isCosmetic();

    default boolean notCosmetic() {
        return !this.isCosmetic();
    }

    double getWeightExponent();

    int getWeightDivider();

    interface Level {

        int getLevel();

        int getTotalRequiredXP();

        @NotNull ConcurrentList<String> getUnlocks();

        default @NotNull ConcurrentMap<String, Double> getEffects() {
            return MinecraftApi.getRepository(Stat.class)
                .stream()
                .map(stat -> Pair.of(
                    stat.getId(),
                    this.getUnlocks()
                        .indexedStream()
                        .mapToObj((line, index, size) -> {
                            String value = "0.0";

                            if (line.startsWith("+") && line.endsWith(stat.getName())) // Flat
                                value = line.split("\\s+")[0];
                            else if (line.contains("Grants +") && line.contains(stat.getName()) && index == size - 1) // Tiered
                                value = line.split("\\s+")[2];

                            value = value.replace("+", "");
                            value = value.replace("%", "");

                            if (value.contains("➜")) // Tiered
                                value = value.split("➜")[1];

                            return value;
                        })
                        .mapToDouble(Double::parseDouble)
                        .sum()
                ))
                .filter(entry -> entry.getValue() > 0.0)
                .collect(Concurrent.toUnmodifiableMap());
        }

    }

}
