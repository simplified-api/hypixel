package api.simplified.hypixel.response.skyblock.member;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Collapse;
import dev.simplified.gson.annotation.Key;
import dev.simplified.gson.annotation.SerializedPath;
import dev.simplified.util.NumberUtil;
import dev.simplified.util.StringUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A member's farming contest history and the perks bought against it.
 *
 * <p>
 * A contest runs for twenty real minutes every three SkyBlock days, with three randomly chosen crops
 * up at once. Placing inside a bracket of the participants earns a medal; gold medals raise the
 * farming level cap one crop at a time, and the top medals across every crop unlock emblems.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Jacob%27s_Farming_Contest">Jacob's Farming Contest</a>
 */
@Getter
public class JacobsContest {

    /**
     * Unspent medals held, keyed by medal. A medal never earned carries no key at all, and the key
     * serialises back as the constant's name rather than the wire's lowercase spelling.
     */
    @SerializedName("medals_inv")
    private @NotNull ConcurrentMap<Medal, Integer> medals = Concurrent.newMap();

    /**
     * Levels of Anita's double-drops perk bought, read out of the wire's {@code perks} node.
     */
    @SerializedPath("perks.double_drops")
    private int doubleDrops;

    /**
     * Extra farming levels bought, one per crop taken to gold, read out of the wire's {@code perks}
     * node. This is what a member's farming skill is capped against.
     */
    @SerializedPath("perks.farming_level_cap")
    private int farmingLevelCap;

    /**
     * Whether the personal-bests perk is owned, read out of the wire's {@code perks} node.
     */
    @Accessors(fluent = true)
    @SerializedPath("perks.personal_bests")
    private boolean hasPersonalBestsPerk;

    /**
     * Whether the member has spoken to Jacob.
     */
    @Accessors(fluent = true)
    @SerializedName("talked")
    private boolean hasTalked;

    /**
     * Every contest the member has entered. The wire's object of contests is collapsed into this
     * list, each key landing on its own contest's id.
     */
    @Collapse
    @SerializedName("contests")
    private @NotNull ConcurrentList<Contest> contests = Concurrent.newList();

    /**
     * Per medal, the crops that medal has been earned in. Keyed by medal, so the keys serialise back
     * as constant names rather than the wire's lowercase spelling.
     */
    @SerializedName("unique_brackets")
    private @NotNull ConcurrentMap<Medal, ConcurrentList<String>> uniqueBrackets = Concurrent.newMap();

    /**
     * Hypixel-side bookkeeping flag for a data migration.
     */
    private boolean migration;

    /**
     * Best single-contest total per crop, keyed by collection id. Those ids carry colons, and
     * {@code INK_SACK:3} is a different collection from {@code INK_SACK}.
     */
    @SerializedName("personal_bests")
    private @NotNull ConcurrentMap<String, Integer> personalBests = Concurrent.newMap();

    /**
     * The medals a contest placing can earn, ranked best first.
     *
     * <p>
     * Declaration order is the ranking and the lookup depends on it -
     * {@link #fromPosition(double, double, boolean)} returns the first constant whose bracket still
     * covers the position, so reordering these silently changes every medal ever computed.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Medals">Medals</a>
     */
    @Getter
    @RequiredArgsConstructor
    public enum Medal {

        /**
         * Reaches down to the top fiftieth of the participants.
         */
        DIAMOND(0.02, 0.05),

        /**
         * Reaches down to the top twentieth of the participants.
         */
        PLATINUM(0.05, 0.1),

        /**
         * Reaches down to the top tenth of the participants, and is the medal that raises the
         * farming level cap.
         */
        GOLD(0.1, 0.2),

        /**
         * Reaches down to the top three tenths of the participants.
         */
        SILVER(0.3, 0.4),

        /**
         * Reaches down to the top three fifths of the participants.
         */
        BRONZE(0.6, 0.7),

        /**
         * No medal. Its bracket covers the whole field, so a placing that reaches none of the others
         * lands here.
         */
        NONE(1.0, 1.0);

        /**
         * Fraction of the participants this medal reaches down to.
         */
        private final double bracket;

        /**
         * Widened fraction of the participants this medal reaches down to when the contest ran under
         * the Finnegan perk.
         */
        private final double finneganBracket;

        /**
         * Computes the medal a contest's placing earns.
         *
         * @param contest the contest to read the placing, the field size and the widened bracket from
         * @return the best medal whose bracket covers the placing
         */
        public static @NotNull Medal fromContest(@NotNull Contest contest) {
            return fromPosition(contest.getPosition(), contest.getParticipants(), contest.isFinnegan());
        }

        /**
         * Computes the medal a placing earns in a field of a given size.
         *
         * <p>
         * The constants are walked in declaration order and the first whose bracket covers the
         * position wins. {@link #NONE} covers the whole field, so a placing inside it always matches.
         *
         * @param position the finishing place
         * @param participants how many entered the contest
         * @param isFinnegan whether to measure against the widened Finnegan brackets
         * @return the best medal whose bracket covers the placing
         */
        public static @NotNull Medal fromPosition(double position, double participants, boolean isFinnegan) {
            for (Medal medal : Medal.values()) {
                double bracket = isFinnegan ? medal.getFinneganBracket() : medal.getBracket();

                if (position <= Math.floor(participants * bracket))
                    return medal;
            }

            return NONE;
        }

    }

    /**
     * One farming contest a member entered.
     *
     * <p>
     * Its id is the wire key, and that key has four colon-separated parts rather than three whenever
     * the crop is itself a colon-bearing collection id - {@code 229:5_31:INK_SACK:3} is year 229,
     * month 5 day 31, collection {@code INK_SACK:3}. Truncating at three parts yields {@code INK_SACK}
     * and reads as a different collection.
     *
     * <p>
     * Only the collected total is on every contest. One whose reward was never claimed binds its
     * placing and its field size to zero, which leaves {@link #getMedal()} with nothing meaningful to
     * compute from.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Jacob%27s_Farming_Contest">Jacob's Farming Contest</a>
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Contest {

        /**
         * Crops farmed during the contest.
         */
        private int collected;

        /**
         * Whether the contest's reward has been taken.
         */
        @Accessors(fluent = true)
        @SerializedName("claimed_rewards")
        private boolean hasClaimedRewards;

        /**
         * Finishing place, zero on a contest whose reward was never claimed.
         */
        @SerializedName("claimed_position")
        private int position;

        /**
         * How many entered the contest, zero on a contest whose reward was never claimed.
         */
        @SerializedName("claimed_participants")
        private int participants;

        /**
         * The contest key - year, calendar date and collection, colon-separated. It is the key this
         * object hung off rather than anything in its own body, so it does not survive a write.
         */
        @Key
        private transient @NotNull String id = "";

        /**
         * The medal the wire itself recorded for the contest. No accessor is generated for it, so its
         * only observable effect is {@link #isFinnegan()}.
         */
        @Getter(AccessLevel.NONE)
        @SerializedName("claimed_medal")
        private @NotNull Optional<Medal> claimedMedal = Optional.empty();

        /**
         * Collection the contest was farmed for, which may itself carry colons - the brown-dye
         * contests are spelled {@code INK_SACK:3}.
         *
         * <p>
         * Derived by joining the id from its third part onward, which is what makes the four-part
         * keys read correctly.
         */
        public @NotNull String getCollectionName() {
            String[] parts = this.getId().split(":");
            return StringUtil.join(parts, ":", 2, parts.length);
        }

        /**
         * SkyBlock date the contest ran on, derived from the year and calendar parts of the id.
         */
        public @NotNull SkyBlockDate getSkyBlockDate() {
            String[] parts = this.getId().split(":");
            String[] calendar = parts[1].split("_");

            return new SkyBlockDate(
                NumberUtil.toInt(parts[0]),
                NumberUtil.toInt(calendar[0]),
                NumberUtil.toInt(calendar[1])
            );
        }

        /**
         * Medal recomputed from the contest's placing and field size, rather than the one the wire
         * recorded.
         */
        public @NotNull Medal getMedal() {
            return Medal.fromContest(this);
        }

        /**
         * Whether the wire recorded a medal of its own for this contest, which is what widens the
         * brackets the placing is measured against.
         */
        public boolean isFinnegan() {
            return this.claimedMedal.isPresent();
        }

    }

}
