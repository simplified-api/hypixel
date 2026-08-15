package api.simplified.hypixel.response.hypixel;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Lenient;
import dev.simplified.gson.annotation.SerializedPath;
import dev.simplified.util.RegexUtil;
import dev.simplified.util.StringUtil;
import lib.minecraft.text.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * A player's network-wide Hypixel account - login history, network currencies, cosmetics,
 * achievements and the several fields their rank is pieced together from.
 */
@Getter
public class HypixelPlayer {

    /**
     * Hypixel's own internal id for the player, distinct from the Minecraft unique id.
     */
    @SerializedName("_id")
    private String hypixelId;

    /**
     * Minecraft unique id of the player.
     */
    @SerializedName("uuid")
    private UUID uniqueId;

    /**
     * Player name as the network last saw it.
     */
    @SerializedName("displayname")
    private String displayName;
    //@SerializedName("playername")
    //private String playerName;

    /**
     * Chat channel the player last had selected.
     */
    @SerializedName("channel")
    private String chatChannel;

    /**
     * When the player first joined the network.
     */
    private Instant firstLogin;

    /**
     * When the player last logged in.
     */
    private Instant lastLogin;

    /**
     * When the player last logged out.
     */
    private Instant lastLogout;

    /**
     * Network experience earned across every game, which the network level is derived from.
     */
    private long networkExp;

    /**
     * Karma the player has accumulated.
     */
    private long karma;

    /**
     * Total achievement points the player has earned.
     */
    private int achievementPoints;

    /**
     * Number of daily rewards the player has claimed.
     */
    private long totalDailyRewards;

    /**
     * Total number of rewards the player has claimed.
     */
    private long totalRewards;

    /**
     * Minecraft client version the player last connected with.
     */
    private String mcVersionRp;

    /**
     * Type name of the game the player most recently played.
     */
    private String mostRecentGameType;

    /**
     * Player names the network has seen this account use.
     */
    private ConcurrentList<String> knownAliases;

    /**
     * Social media accounts the player has linked.
     */
    private HypixelSocial socialMedia;

    /**
     * One-time achievements the player has unlocked. Entries that do not bind fall into overflow
     * rather than failing the decode.
     */
    @Lenient
    private ConcurrentList<String> achievementsOneTime = Concurrent.newList();

    /**
     * Cosmetic click effect the player currently has equipped.
     */
    private String currentClickEffect;

    /**
     * Cosmetic gadget the player currently has equipped.
     */
    private String currentGadget;

    /**
     * When the player claimed the potato talisman.
     */
    @SerializedName("claimed_potato_talisman")
    private Instant claimedPotatoTalisman;

    /**
     * When the player was granted a free SkyBlock booster cookie.
     */
    @SerializedName("skyblock_free_cookie")
    private Instant skyblockFreeCookie;

    /**
     * When the player claimed the century cake.
     */
    @SerializedName("claimed_century_cake")
    private Instant claimedCenturyCake;

    /**
     * When the player collected the Scorpius bribe tied to year 120.
     */
    @SerializedName("scorpius_bribe_120")
    private Instant scorpiusBribe120;

    /**
     * Server-list voting counters and timestamps, keyed by their wire name.
     */
    private ConcurrentMap<String, Long> voting = Concurrent.newMap();

    /**
     * Lobby pet consumables the player holds, keyed by consumable name.
     */
    private ConcurrentMap<String, Integer> petConsumables = Concurrent.newMap();

    /**
     * Progress on each tiered achievement, keyed by achievement name.
     */
    private ConcurrentMap<String, Integer> achievements = Concurrent.newMap();

    /**
     * When each achievement point reward was handed out, keyed by the reward's name.
     */
    private ConcurrentMap<String, Instant> achievementRewardsNew = Concurrent.newMap();

    // Rank

    /**
     * Oldest of the purchased rank fields, outranked by every later rank field that carries a value.
     */
    @Getter(AccessLevel.NONE)
    private String packageRank;

    /**
     * Purchased rank, ignored when it reads {@code NONE}.
     */
    @Getter(AccessLevel.NONE)
    private String newPackageRank;

    /**
     * Monthly subscription rank, ignored when it reads {@code NONE}.
     */
    @Getter(AccessLevel.NONE)
    private String monthlyPackageRank;

    /**
     * Staff or special rank, ignored when it reads {@code NORMAL}.
     */
    @Getter(AccessLevel.NONE)
    private String rank;

    /**
     * Fully formatted rank prefix, which wins over every other rank field when the player carries one.
     */
    @Getter(AccessLevel.NONE)
    private String prefix;

    /**
     * Colour the rank name renders in, read only for {@link HypixelRank.Type#SUPERSTAR}.
     */
    @Getter(AccessLevel.NONE)
    private String monthlyRankColor;

    /**
     * Colour the rank's plus signs render in.
     */
    @Getter(AccessLevel.NONE)
    private String rankPlusColor;

    /**
     * Most recent monthly subscription rank the player held.
     */
    @Getter(AccessLevel.NONE)
    private String mostRecentMonthlyPackageRank;

    // Stats (Only SkyBlock Currently)

    /**
     * The player's SkyBlock profiles keyed by profile id, bound from a nested wire node under the
     * player's SkyBlock stats.
     */
    @Getter(AccessLevel.NONE)
    @SerializedPath("stats.SkyBlock.profiles")
    private @NotNull ConcurrentMap<String, SkyBlockProfile> skyBlockProfiles = Concurrent.newMap();

    /**
     * The player's rank, derived by reading the rank fields in ascending precedence and then colouring
     * the result from the monthly and plus colours.
     */
    public @NotNull HypixelRank getRank() {
        HypixelRank.Type type = HypixelRank.Type.NONE;

        if (StringUtil.isNotEmpty(this.packageRank))
            type = HypixelRank.Type.findByName(this.packageRank).orElse(HypixelRank.Type.NONE);

        if (StringUtil.isNotEmpty(this.newPackageRank) && !"NONE".equals(this.newPackageRank))
            type = HypixelRank.Type.findByName(this.newPackageRank).orElse(HypixelRank.Type.NONE);

        if (StringUtil.isNotEmpty(this.monthlyPackageRank) && !"NONE".equals(this.monthlyPackageRank))
            type = HypixelRank.Type.findByName(this.monthlyPackageRank).orElse(HypixelRank.Type.NONE);

        if (StringUtil.isNotEmpty(this.rank) && !"NORMAL".equals(this.rank))
            type = HypixelRank.Type.findByName(this.rank).orElse(HypixelRank.Type.NONE);

        if (StringUtil.isNotEmpty(this.prefix))
            type = HypixelRank.Type.findByName(RegexUtil.strip(this.prefix, RegexUtil.VANILLA_PATTERN).replaceAll("[\\W]", ""))
                .orElse(HypixelRank.Type.NONE);

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
     * The player's SkyBlock profiles, in wire order.
     */
    public @NotNull ConcurrentList<SkyBlockProfile> getSkyBlockProfiles() {
        return Concurrent.newUnmodifiableList(this.skyBlockProfiles.values());
    }

    /**
     * The identity of one SkyBlock profile the player belongs to.
     */
    @Getter
    public static class SkyBlockProfile {

        /**
         * Unique id of the profile.
         */
        @SerializedName("profile_id")
        private UUID islandId;

        /**
         * Randomly generated name the profile is shown under.
         */
        @SerializedName("cute_name")
        private String profileName;

    }

}