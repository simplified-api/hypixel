package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Getter
public class JacobsContest {

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
    private @NotNull ConcurrentList<Contest> contestList = Concurrent.newList();
    @SerializedName("unique_brackets")
    private @NotNull ConcurrentMap<Medal, ConcurrentList<String>> uniqueBrackets = Concurrent.newMap();
    private boolean migration;
    @SerializedName("personal_bests")
    private @NotNull ConcurrentMap<String, Integer> personalBests = Concurrent.newMap();

    public @NotNull ConcurrentList<Contest> getContests() {
        if (this.contestList.isEmpty()) {
            this.contestList = this.contestMap.stream()
                .map(entry -> {
                    Contest contest = entry.getValue();

                    String[] dataString = entry.getKey().split(":");
                    String[] calendarString = dataString[1].split("_");
                    int year = NumberUtil.toInt(dataString[0]);
                    int month = NumberUtil.toInt(calendarString[0]);
                    int day = NumberUtil.toInt(calendarString[1]);
                    String collectionName = StringUtil.join(dataString, ":", 2, dataString.length);

                    contest.skyBlockDate = new SkyBlockDate(year, month, day);
                    contest.collectionName = collectionName;
                    return contest;
                })
                .collect(Concurrent.toList())
                .toUnmodifiableList();
        }

        return this.contestList;
    }

    public enum Medal {

        BRONZE,
        SILVER,
        GOLD,
        PLATINUM,
        DIAMOND

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

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Data {

            private SkyBlockDate skyBlockDate;
            private String collectionName;

        }

    }

}
