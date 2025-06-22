package dev.sbs.minecraftapi.client.hypixel.response.hypixel.player;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.util.RegexUtil;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

@Getter
public class HypixelPlayer {

    @SerializedName("_id")
    private String hypixelId;
    @SerializedName("uuid")
    private UUID uniqueId;
    @SerializedName("displayname")
    private String displayName;
    //@SerializedName("playername")
    //private String playerName;
    @SerializedName("channel")
    private String chatChannel;
    private Instant firstLogin;
    private Instant lastLogin;
    private Instant lastLogout;
    private long networkExp;
    private long karma;
    private int achievementPoints;
    private long totalDailyRewards;
    private long totalRewards;
    private String mcVersionRp;
    private String mostRecentGameType;
    private ConcurrentList<String> knownAliases;
    private HypixelSocial socialMedia;
    @Getter(AccessLevel.NONE)
    private ConcurrentList<Object> achievementsOneTime = Concurrent.newList();
    @Getter(AccessLevel.NONE)
    private transient ConcurrentList<String> achievementsOneTimeFixed;
    private String currentClickEffect;
    private String currentGadget;
    @SerializedName("claimed_potato_talisman")
    private Instant claimedPotatoTalisman;
    @SerializedName("skyblock_free_cookie")
    private Instant skyblockFreeCookie;
    @SerializedName("claimed_century_cake")
    private Instant claimedCenturyCake;
    @SerializedName("scorpius_bribe_120")
    private Instant scorpiusBribe120;
    private ConcurrentMap<String, Long> voting = Concurrent.newMap();
    private ConcurrentMap<String, Integer> petConsumables = Concurrent.newMap();
    private ConcurrentMap<String, Integer> achievements = Concurrent.newMap();
    private ConcurrentMap<String, Instant> achievementRewardsNew = Concurrent.newMap();

    // Rank
    @Getter(AccessLevel.NONE)
    private String packageRank;
    @Getter(AccessLevel.NONE)
    private String newPackageRank;
    @Getter(AccessLevel.NONE)
    private String monthlyPackageRank;
    @Getter(AccessLevel.NONE)
    private String rank;
    @Getter(AccessLevel.NONE)
    private String prefix;
    @Getter(AccessLevel.NONE)
    private String monthlyRankColor;
    @Getter(AccessLevel.NONE)
    private String rankPlusColor;
    @Getter(AccessLevel.NONE)
    private String mostRecentMonthlyPackageRank;

    // Stats (Only SkyBlock Currently)
    private Stats stats;

    public ConcurrentList<String> getAchievementsOneTime() {
        if (this.achievementsOneTimeFixed == null) {
            this.achievementsOneTimeFixed = this.achievementsOneTime.stream()
                .filter(String.class::isInstance)
                .map(String::valueOf)
                .collect(Concurrent.toList());
        }

        return this.achievementsOneTimeFixed;
    }

    public @NotNull HypixelRank getRank() {
        HypixelRank.Type type = HypixelRank.Type.NONE;

        if (StringUtil.isNotEmpty(this.packageRank))
            type = HypixelRank.Type.of(this.packageRank);

        if (StringUtil.isNotEmpty(this.newPackageRank))
            type = HypixelRank.Type.of(this.newPackageRank);

        if (StringUtil.isNotEmpty(this.monthlyPackageRank) && !"NONE".equals(this.monthlyPackageRank))
            type = HypixelRank.Type.of(this.monthlyPackageRank);

        if (StringUtil.isNotEmpty(this.rank) && !"NORMAL".equals(this.rank))
            type = HypixelRank.Type.of(this.rank);

        if (StringUtil.isNotEmpty(this.prefix))
            type = HypixelRank.Type.of(RegexUtil.strip(this.prefix, RegexUtil.VANILLA_PATTERN).replaceAll("[\\W]", ""));

        ChatFormat rankFormat = type.getFormat();
        ChatFormat plusFormat = type.getFormat();

        if (type == HypixelRank.Type.SUPERSTAR && StringUtil.isNotEmpty(this.monthlyRankColor))
            rankFormat = ChatFormat.valueOf(this.monthlyRankColor);

        if (StringUtil.isNotEmpty(this.rankPlusColor))
            plusFormat = ChatFormat.valueOf(this.rankPlusColor);

        if (type == HypixelRank.Type.PIG)
            plusFormat = ChatFormat.AQUA;

        return new HypixelRank(type, rankFormat, plusFormat);
    }

    @Getter
    public static class Stats {

        @SerializedName("SkyBlock")
        private SkyBlock skyBlock;

        public static class SkyBlock {

            private @NotNull ConcurrentMap<String, Profile> profiles = Concurrent.newMap();

            public @NotNull ConcurrentList<Profile> getProfiles() {
                return Concurrent.newUnmodifiableList(this.profiles.values());
            }


            @Getter
            public static class Profile {

                @SerializedName("profile_id")
                private UUID islandId;
                @SerializedName("cute_name")
                private String profileName;

            }

        }

    }

}