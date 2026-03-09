package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class JacobsContest implements PostInit {

    @SerializedName("medals_inv")
    private @NotNull ConcurrentMap<Medal, Integer> medals = Concurrent.newMap();
    @SerializedPath("perks.double_drops")
    private int doubleDrops;
    @SerializedPath("perks.farming_level_cap")
    private int farmingLevelCap;
    @Accessors(fluent = true)
    @SerializedName("talked")
    private boolean hasTalked;
    @Getter(AccessLevel.NONE)
    @SerializedName("contests")
    private @NotNull ConcurrentMap<String, Contest> contestMap = Concurrent.newMap();
    private transient @NotNull ConcurrentList<Contest> contests = Concurrent.newList();
    @SerializedName("unique_brackets")
    private @NotNull ConcurrentMap<Medal, ConcurrentList<String>> uniqueBrackets = Concurrent.newMap();
    private boolean migration;
    @SerializedName("personal_bests")
    private @NotNull ConcurrentMap<String, Integer> personalBests = Concurrent.newMap();

    @Override
    public void postInit() {
        this.contests = this.contestMap.stream()
            .map(entry -> {
                Contest contest = entry.getValue();

                String[] dataString = entry.getKey().split(":");
                String[] calendarString = dataString[1].split("_");
                int year = NumberUtil.toInt(dataString[0]);
                int month = NumberUtil.toInt(calendarString[0]);
                int day = NumberUtil.toInt(calendarString[1]);

                contest.collectionName = StringUtil.join(dataString, ":", 2, dataString.length);
                contest.skyBlockDate = new SkyBlockDate(year, month, day);
                return contest;
            })
            .collect(Concurrent.toUnmodifiableList());
    }

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
        private SkyBlockDate skyBlockDate;
        private String collectionName;

        @Getter(AccessLevel.NONE)
        @SerializedName("claimed_medal")
        private @NotNull Optional<Medal> claimedMedal = Optional.empty();

        public @NotNull Medal getMedal() {
            return Medal.fromContest(this);
        }

        public boolean isFinnegan() {
            return this.claimedMedal.isPresent();
        }

    }

}
