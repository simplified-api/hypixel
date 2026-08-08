package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.SkyBlockData;
import dev.sbs.skyblockdata.model.BestiaryFamily;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Lenient;
import dev.simplified.gson.PostInit;
import dev.simplified.gson.annotation.SerializedPath;
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

    private static final @NotNull Pattern MOB_PATTERN = Pattern.compile("^(.*)_([0-9]+)$");
    @SerializedName("migrated_stats")
    private boolean migratedStats;
    private boolean migration;
    @Extract("kills.last_killed_mob")
    private @NotNull Optional<String> lastKilledMob = Optional.empty();
    @SerializedPath("milestone.last_claimed_milestone")
    private int lastClaimedMilestone;
    @SerializedPath("miscellaneous.max_kills_visible")
    private boolean maxKillsVisible;
    @Accessors(fluent = true)
    @SerializedPath("miscellaneous.milestones_notifications")
    private boolean hasNotificationsEnabled;
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
        // one mob per distinct key, so a mob carrying both a kill and a death count is not counted twice,
        // and one matcher per key, so the groups are only read off a matcher that has matched
        ConcurrentList<Mob> mobs = Stream.concat(this.kills.keySet().stream(), this.deaths.keySet().stream())
            .distinct()
            .map(MOB_PATTERN::matcher)
            .filter(Matcher::matches)
            .map(matcher -> new Mob(
                matcher.group(1).toUpperCase(),
                NumberUtil.tryParseInt(matcher.group(2)),
                this.kills.getOrDefault(matcher.group(), 0),
                this.deaths.getOrDefault(matcher.group(), 0)
            ))
            .collect(Concurrent.toUnmodifiableList());

        this.families = SkyBlockData.getRepository(BestiaryFamily.class)
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
            return SkyBlockData.getRepository(BestiaryFamily.class)
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
            return SkyBlockData.getRepository(BestiaryFamily.class)
                .matchFirstOrNull(family -> family.getMobs().contains(String.format("%s_%s", this.getId(), this.getLevel())));
        }

    }

}
