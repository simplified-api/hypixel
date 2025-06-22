package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.bestiary;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.data.model.bestiary_data.bestiary.BestiaryModel;
import dev.sbs.minecraftapi.data.model.bestiary_data.bestiary_brackets.BestiaryBracketModel;
import dev.sbs.api.stream.pair.Pair;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;
import java.util.stream.Stream;

@Getter
public class EnhancedBestiary extends Bestiary {

    private final @NotNull ConcurrentList<Family> families;

    EnhancedBestiary(@NotNull Bestiary bestiary) {
        // Re-initialize Fields
        super(
            bestiary.getKills(),
            bestiary.getDeaths(),
            bestiary.getLastClaimedMilestone()
        );

        this.families = SimplifiedApi.getRepositoryOf(BestiaryModel.class)
            .stream()
            .map(bestiaryModel -> new Family(
                bestiaryModel,
                this.getKills(),
                this.getDeaths()
            ))
            .collect(Concurrent.toUnmodifiableList());
    }

    public int getMilestone() {
        return this.getUnlocked() / 10;
    }

    public int getUnlocked() {
        return this.getFamilies()
            .stream()
            .mapToInt(Family::getLevel)
            .sum();
    }

    @Getter
    @RequiredArgsConstructor
    public static class Family {

        private final @NotNull BestiaryModel type;
        private final @NotNull ConcurrentList<Mob> mobs;
        private final @NotNull ConcurrentList<Integer> tiers;

        public Family(@NotNull BestiaryModel type, @NotNull ConcurrentMap<String, Integer> kills, @NotNull ConcurrentMap<String, Integer> deaths) {
            this.type = type;
            ConcurrentMap<String, Integer> patterns = buildPatterns(type);

            this.mobs = Stream.concat(
                    kills.stream()
                        .filterKey(patterns::containsKey)
                        .map(Pair::of),
                    deaths.stream()
                        .filterKey(patterns::containsKey)
                        .map(Pair::of)
                )
                .distinct()
                .map(entry -> new Mob(
                    type.getInternalPattern(),
                    entry.getKey(),
                    entry.getValue(),
                    kills.getOrDefault(entry.getKey(), 0),
                    deaths.getOrDefault(entry.getKey(), 0)
                ))
                .collect(Concurrent.toUnmodifiableList());

            this.tiers = SimplifiedApi.getRepositoryOf(BestiaryBracketModel.class)
                .findAll(BestiaryBracketModel::getBracket, this.getBracket().getBracket())
                .map(BestiaryBracketModel::getTotalKillsRequired)
                .collect(Concurrent.toUnmodifiableList());
        }

        private static @NotNull ConcurrentMap<String, Integer> buildPatterns(@NotNull BestiaryModel type) {
            return type.getLevels()
                .stream()
                .map(level -> Pair.of(String.format("^%s_%s$", type.getInternalPattern().toLowerCase(), level), level))
                .collect(Concurrent.toMap());
        }

        public @NotNull BestiaryBracketModel getBracket() {
            return this.getType().getBracket();
        }

        public int getLevel() {
            int totalKills = this.getMobs()
                .stream()
                .mapToInt(Mob::getKills)
                .sum();

            return Math.min(
                this.getMaxLevel(),
                IntStream.range(0, this.getTiers().size())
                    .filter(index -> this.getTiers().get(index) > totalKills)
                    .findFirst()
                    .orElse(0)
            );
        }

        public int getMaxLevel() {
            return this.getBracket().getTier();
        }

    }

    @Getter
    @RequiredArgsConstructor
    public static class Mob {

        private final @NotNull String internalPattern;
        private final @NotNull String matchedName;
        private final int level;
        private final int kills;
        private final int deaths;

    }

}
