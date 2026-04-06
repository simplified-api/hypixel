package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.persistence.model.BestiaryFamily;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.PairStream;
import dev.simplified.gson.Extract;
import dev.simplified.gson.Lenient;
import dev.simplified.gson.PostInit;
import dev.simplified.gson.SerializedPath;
import dev.simplified.util.NumberUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Getter
public class Bestiary implements PostInit {

    private static final @NotNull Pattern MOB_PATTERN = Pattern.compile("^([a-z_]+)_([0-9]+)$");
    @SerializedName("migrated_stats")
    private boolean migratedStats;
    private boolean migration;
    @Extract("kills.last_killed_mob")
    private @NotNull Optional<String> lastKilledMob = Optional.empty();
    @SerializedPath("milestone.last_claimed_milestone")
    private int lastClaimedMilestone;
    private @NotNull Miscellaneous miscellaneous = new Miscellaneous();
    @Lenient
    private @NotNull ConcurrentMap<String, Integer> kills = Concurrent.newMap();
    @Lenient
    private @NotNull ConcurrentMap<String, Integer> deaths = Concurrent.newMap();
    private transient @NotNull ConcurrentList<Family> families = Concurrent.newList();

    public int getMilestone() {
        return this.getUnlocked() / 10;
    }

    public int getUnlocked() {
        return this.getFamilies()
            .stream()
            .mapToInt(Family::getLevel)
            .sum();
    }

    @Override
    public void postInit() {
        ConcurrentList<Mob> mobs = PairStream.of(Stream.concat(this.kills.stream(), this.deaths.stream()))
            .distinct()
            .filterKey(key -> MOB_PATTERN.matcher(key).matches())
            .collapseToSingle((key, value) -> {
                Matcher matcher = MOB_PATTERN.matcher(key);
                String id = matcher.group(1).toUpperCase();
                int level = NumberUtil.tryParseInt(matcher.group(2));

                return new Mob(
                    id,
                    level,
                    this.kills.getOrDefault(key, 0),
                    this.deaths.getOrDefault(key, 0)
                );
            })
            .collect(Concurrent.toUnmodifiableList());

        this.families = MinecraftApi.getRepository(BestiaryFamily.class)
            .stream()
            .map(family -> new Family(
                family.getId(),
                mobs.stream()
                    .filter(mob -> mob.getFamily().equals(family))
                    .collect(Concurrent.toUnmodifiableList())
            ))
            .collect(Concurrent.toUnmodifiableList());
    }

    @Getter
    @RequiredArgsConstructor
    public static class Family {

        private final @NotNull String familyId;
        private final @NotNull ConcurrentList<Mob> mobs;

        public @NotNull BestiaryFamily getType() {
            return MinecraftApi.getRepository(BestiaryFamily.class)
                .findFirstOrNull(BestiaryFamily::getId, this.getFamilyId());
        }

        public @NotNull ConcurrentList<Integer> getTiers() {
            return this.getType().getTiers();
        }

        public int getBracket() {
            return this.getType().getBracket();
        }

        public int getLevel() {
            return Math.min(
                this.getMaxTier(),
                IntStream.range(0, this.getTiers().size())
                    .filter(index -> this.getTiers().get(index) > this.getMobs()
                        .stream()
                        .mapToInt(Mob::getKills)
                        .sum()
                    )
                    .findFirst()
                    .orElse(0)
            );
        }

        public int getMaxTier() {
            return this.getType().getMaxTier();
        }

    }

    @Getter
    @RequiredArgsConstructor
    public static class Mob {

        private final @NotNull String id;
        private final int level;
        private final int kills;
        private final int deaths;

        public @NotNull BestiaryFamily getFamily() {
            return MinecraftApi.getRepository(BestiaryFamily.class)
                .matchFirstOrNull(family -> family.getMobs().contains(String.format("%s_%s", this.getId(), this.getLevel())));
        }

    }

    @Getter
    private static class Miscellaneous {

        @SerializedName("max_kills_visible")
        private boolean maxKillsVisible;
        @Accessors(fluent = true)
        @SerializedName("milestones_notifications")
        private boolean hasNotificationsEnabled;

    }

}
