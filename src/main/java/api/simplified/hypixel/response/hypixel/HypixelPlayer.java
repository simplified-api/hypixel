package api.simplified.hypixel.response.hypixel;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Lenient;
import dev.simplified.gson.annotation.SerializedPath;
import dev.simplified.util.RegexUtil;
import dev.simplified.util.StringUtil;
import lib.minecraft.text.ChatColor;
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
    // the list interleaves achievement names with entries of other shapes, so typing it takes the
    // names and leaves the rest in overflow, where they round-trip
    @Lenient
    private ConcurrentList<String> achievementsOneTime = Concurrent.newList();
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
    @Getter(AccessLevel.NONE)
    @SerializedPath("stats.SkyBlock.profiles")
    private @NotNull ConcurrentMap<String, SkyBlockProfile> skyBlockProfiles = Concurrent.newMap();

    public @NotNull HypixelRank getRank() {
        HypixelRank.Type type = HypixelRank.Type.NONE;

        if (StringUtil.isNotEmpty(this.packageRank))
            type = HypixelRank.Type.of(this.packageRank);

        if (StringUtil.isNotEmpty(this.newPackageRank) && !"NONE".equals(this.newPackageRank))
            type = HypixelRank.Type.of(this.newPackageRank);

        if (StringUtil.isNotEmpty(this.monthlyPackageRank) && !"NONE".equals(this.monthlyPackageRank))
            type = HypixelRank.Type.of(this.monthlyPackageRank);

        if (StringUtil.isNotEmpty(this.rank) && !"NORMAL".equals(this.rank))
            type = HypixelRank.Type.of(this.rank);

        if (StringUtil.isNotEmpty(this.prefix))
            type = HypixelRank.Type.of(RegexUtil.strip(this.prefix, RegexUtil.VANILLA_PATTERN).replaceAll("[\\W]", ""));

        ChatColor rankFormat = type.getColor();
        ChatColor plusFormat = type.getColor();

        if (type == HypixelRank.Type.SUPERSTAR && StringUtil.isNotEmpty(this.monthlyRankColor))
            rankFormat = ChatColor.of(this.monthlyRankColor);

        if (StringUtil.isNotEmpty(this.rankPlusColor))
            plusFormat = ChatColor.of(this.rankPlusColor);

        if (type == HypixelRank.Type.PIG)
            plusFormat = ChatColor.Legacy.AQUA;

        return new HypixelRank(type, rankFormat, plusFormat);
    }

    /**
     * The player's SkyBlock profiles, keyed by island id on the wire and returned in wire order
     */
    public @NotNull ConcurrentList<SkyBlockProfile> getSkyBlockProfiles() {
        return Concurrent.newUnmodifiableList(this.skyBlockProfiles.values());
    }

    @Getter
    public static class SkyBlockProfile {

        @SerializedName("profile_id")
        private UUID islandId;
        @SerializedName("cute_name")
        private String profileName;

    }

}