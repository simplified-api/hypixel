package api.simplified.hypixel.response.skyblock.member;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.BestiaryFamily;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NamingStyle;
import dev.simplified.annotations.RequiredArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Lenient;
import dev.simplified.gson.annotation.SerializedPath;
import dev.simplified.util.NumberUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A member's kill and death tally against every mob they have fought.
 *
 * <p>
 * Mobs group into families, a family levels on its cumulative kills against a bracketed tier ladder,
 * and every ten family levels is one milestone. Only the tallies are bound - the mobs, the families,
 * the levels and the milestone are all derived from them.
 *
 * <p>
 * {@link #getFamilies()} is the repository boundary and needs a session. The tallies are not; the
 * parse that turns {@link #kills} and {@link #deaths} into mobs completes with nothing open.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Bestiary">Bestiary</a>
 */
@Getter
public class Bestiary {

    /**
     * Splits a tally key into a mob id and the level it was kept against, at the last underscore
     * before the digits - {@code master_lost_adventurer_131} is level 131 of
     * {@code MASTER_LOST_ADVENTURER}, not level 1 of anything.
     */
    private static final @NotNull Pattern MOB_PATTERN = Pattern.compile("^(.*)_([0-9]+)$");

    /**
     * Hypixel-side bookkeeping flag for a data migration.
     */
    @SerializedName("migrated_stats")
    private boolean migratedStats;

    /**
     * A second Hypixel-side bookkeeping flag for a data migration.
     */
    private boolean migration;

    /**
     * The mob most recently killed, its id and level together. It is lifted out of the kill tally
     * rather than bound from a root key of its own, because its value is a string sitting in an
     * otherwise numeric map.
     */
    @Extract("kills.last_killed_mob")
    private @NotNull Optional<String> lastKilledMob = Optional.empty();

    /**
     * Highest milestone whose reward has been taken, read out of the wire's {@code milestone} node.
     */
    @SerializedPath("milestone.last_claimed_milestone")
    private int lastClaimedMilestone;

    /**
     * Menu toggle for showing the kill cap, read out of the wire's {@code miscellaneous} node.
     */
    @SerializedPath("miscellaneous.max_kills_visible")
    private boolean maxKillsVisible;

    /**
     * Whether milestone chat notifications are switched on, read out of the wire's
     * {@code miscellaneous} node.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedPath("miscellaneous.milestones_notifications")
    private boolean hasNotificationsEnabled;

    /**
     * Kills per mob, keyed {@code <mob>_<level>}. Entries whose value is not a number fall into
     * overflow rather than failing the decode.
     */
    @Lenient
    private @NotNull ConcurrentMap<String, Integer> kills = Concurrent.newMap();

    /**
     * Deaths per mob, keyed the same way as the kills and just as forgiving of an entry it cannot
     * bind.
     */
    @Lenient
    private @NotNull ConcurrentMap<String, Integer> deaths = Concurrent.newMap();

    @Getter(AccessLevel.NONE)
    private transient ConcurrentList<Family> families;

    /**
     * Milestones earned, one for every ten family levels.
     *
     * <p>
     * Derived by integer division over every family, so it walks the whole repository and needs a
     * session. It is recomputed rather than read, and need not agree with the milestone the wire
     * reports as claimed.
     */
    public int getMilestone() {
        return this.getUnlocked() / 10;
    }

    /**
     * Sum of every family's level.
     *
     * <p>
     * Derived, and resolved through the families, so it needs a session.
     */
    public int getUnlocked() {
        return this.getFamilies()
            .stream()
            .mapToInt(Family::getLevel)
            .sum();
    }

    /**
     * Every bestiary family, each carrying the mobs of its own that this member has fought.
     *
     * <p>
     * Two steps in order: the distinct keys of the kill and death tallies are parsed into mobs,
     * which opens nothing, and those mobs are then joined onto the family repository, which needs a
     * session.
     *
     * <p>
     * Memoised because {@link Family#getType()} and the three accessors behind it re-query the
     * repository per call, and {@link #getUnlocked()} walks every family. The two racers compute the
     * same value from bound fields, so the race is benign.
     */
    public @NotNull ConcurrentList<Family> getFamilies() {
        if (this.families == null) {
            // one mob per distinct key, so a mob carrying both a kill and a death count is not counted
            // twice, and one matcher per key, so groups are only read off a matcher that has matched
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
                        .filter(mob -> family.getMobs().contains(mob.getKey()))
                        .collect(Concurrent.toUnmodifiableList())
                ))
                .collect(Concurrent.toUnmodifiableList());
        }

        return this.families;
    }

    /**
     * One bestiary family and the mobs of it this member has a tally against.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Bestiary">Bestiary</a>
     */
    @Getter
    @RequiredArgsConstructor
    public static class Family {

        /**
         * Repository id of the family.
         */
        private final @NotNull String familyId;

        /**
         * Mobs of this family the member has recorded a kill or a death against.
         */
        private final @NotNull ConcurrentList<Mob> mobs;

        /**
         * Repository row backing this family - its tier thresholds, bracket and maximum tier.
         *
         * <p>
         * Resolved, so it needs a session, and every accessor below it goes back through this one.
         */
        public @NotNull BestiaryFamily getType() {
            return SkyBlockData.getRepository(BestiaryFamily.class)
                .findFirstOrNull(BestiaryFamily::getId, this.getFamilyId());
        }

        /**
         * Cumulative kill thresholds the family levels against, one per tier.
         */
        public @NotNull ConcurrentList<Integer> getTiers() {
            return this.getType().getTiers();
        }

        /**
         * Bracket the repository groups this family under.
         */
        public int getBracket() {
            return this.getType().getBracket();
        }

        /**
         * The first tier index whose threshold still exceeds this family's total kills, held down to
         * the family's maximum tier.
         *
         * <p>
         * Derived from the kill tally and resolved against the repository, so it needs a session.
         */
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

        /**
         * Highest tier the family defines.
         */
        public int getMaxTier() {
            return this.getType().getMaxTier();
        }

    }

    /**
     * One mob at one level, with the member's kills and deaths against it.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Bestiary">Bestiary</a>
     */
    @Getter
    @RequiredArgsConstructor
    public static class Mob {

        /**
         * Mob id, uppercased as the tally key was parsed.
         */
        private final @NotNull String id;

        /**
         * Level of the mob the tally was kept against.
         */
        private final int level;

        /**
         * Times the member has killed this mob at this level.
         */
        private final int kills;

        /**
         * Times this mob has killed the member at this level.
         */
        private final int deaths;

        /**
         * How a family's own mob list spells this mob, which is its id and its level joined and
         * lower-cased - the spelling the reference data uses, and the one the tally key arrived in
         * before {@link #getId()} raised it to the module's own convention.
         */
        public @NotNull String getKey() {
            return String.format("%s_%s", this.getId(), this.getLevel()).toLowerCase(Locale.ROOT);
        }

        /**
         * Family whose own mob list names this mob, empty for a mob no family claims.
         *
         * <p>
         * A tally key is whatever the member has fought, so it reaches mobs the bestiary does not
         * rank; resolved by scanning the repository rather than by a keyed lookup, so it needs a
         * session.
         */
        public @NotNull Optional<BestiaryFamily> getFamily() {
            return SkyBlockData.getRepository(BestiaryFamily.class)
                .matchFirst(family -> family.getMobs().contains(this.getKey()));
        }

    }

}
