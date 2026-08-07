package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.date.SkyBlockDate;
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

@Getter
public class JacobsContest {

    @SerializedName("medals_inv")
    private @NotNull ConcurrentMap<Medal, Integer> medals = Concurrent.newMap();
    @SerializedPath("perks.double_drops")
    private int doubleDrops;
    @SerializedPath("perks.farming_level_cap")
    private int farmingLevelCap;
    @Accessors(fluent = true)
    @SerializedPath("perks.personal_bests")
    private boolean hasPersonalBestsPerk;
    @Accessors(fluent = true)
    @SerializedName("talked")
    private boolean hasTalked;
    @Collapse
    @SerializedName("contests")
    private @NotNull ConcurrentList<Contest> contests = Concurrent.newList();
    @SerializedName("unique_brackets")
    private @NotNull ConcurrentMap<Medal, ConcurrentList<String>> uniqueBrackets = Concurrent.newMap();
    private boolean migration;
    @SerializedName("personal_bests")
    private @NotNull ConcurrentMap<String, Integer> personalBests = Concurrent.newMap();

    @Getter
    @RequiredArgsConstructor
    public enum Medal {

        DIAMOND(0.02, 0.05),
        PLATINUM(0.05, 0.1),
        GOLD(0.1, 0.2),
        SILVER(0.3, 0.4),
        BRONZE(0.6, 0.7),
        NONE(1.0, 1.0);

        private final double bracket;
        private final double finneganBracket;

        public static @NotNull Medal fromContest(@NotNull Contest contest) {
            return fromPosition(contest.getPosition(), contest.getParticipants(), contest.isFinnegan());
        }

        public static @NotNull Medal fromPosition(double position, double participants, boolean isFinnegan) {
            for (Medal medal : Medal.values()) {
                double bracket = isFinnegan ? medal.getFinneganBracket() : medal.getBracket();

                if (position <= Math.floor(participants * bracket))
                    return medal;
            }

            return NONE;
        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Contest {

        private int collected;
        @Accessors(fluent = true)
        @SerializedName("claimed_rewards")
        private boolean hasClaimedRewards;
        @SerializedName("claimed_position")
        private int position;
        @SerializedName("claimed_participants")
        private int participants;
        @Key
        private transient @NotNull String id = "";

        @Getter(AccessLevel.NONE)
        @SerializedName("claimed_medal")
        private @NotNull Optional<Medal> claimedMedal = Optional.empty();

        /**
         * Collection the contest was farmed for, which may itself carry colons - the brown-dye
         * contests are spelled {@code INK_SACK:3}
         */
        public @NotNull String getCollectionName() {
            String[] parts = this.getId().split(":");
            return StringUtil.join(parts, ":", 2, parts.length);
        }

        /**
         * SkyBlock date the contest ran on
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

        public @NotNull Medal getMedal() {
            return Medal.fromContest(this);
        }

        public boolean isFinnegan() {
            return this.claimedMedal.isPresent();
        }

    }

}
