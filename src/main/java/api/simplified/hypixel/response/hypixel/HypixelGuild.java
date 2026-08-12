package api.simplified.hypixel.response.hypixel;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lib.minecraft.text.ChatColor;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A guild on the Hypixel network, together with its roster, its own rank ladder and the experience
 * its members have earned for it.
 */
@Getter
public class HypixelGuild {

    /**
     * Guild experience each successive level costs, the final entry repeating for every level past
     * the end of the table.
     */
    private final static ConcurrentList<Integer> HYPIXEL_GUILD_EXP = Concurrent.newList(
        100_000, 150_000, 250_000, 500_000, 750_000, 1_000_000, 1_250_000, 1_500_000,
        2_000_000, 2_500_000, 2_500_000, 2_500_000, 2_500_000, 2_500_000, 3_000_000
    );

    /**
     * Unique id of the guild.
     */
    @SerializedName("_id")
    private String guildId;

    /**
     * Name of the guild.
     */
    private String name;

    /**
     * Short tag rendered beside a member's name, absent when the guild has set none.
     */
    private @NotNull Optional<String> tag = Optional.empty();

    /**
     * Description the guild shows in the guild listing, absent when the guild has set none.
     */
    private @NotNull Optional<String> description = Optional.empty();

    /**
     * Epoch milliseconds at which the guild chat mute expires, zero when guild chat is not muted.
     */
    private long chatMute;

    /**
     * Guild coins currently held.
     */
    private int coins;

    /**
     * Guild coins earned over the guild's whole lifetime.
     */
    private int coinsEver;

    /**
     * When the guild was created.
     */
    private Instant created;

    /**
     * Whether the guild is publicly listed.
     */
    private boolean publiclyListed;

    /**
     * {@link ChatColor} the guild tag renders in, absent when the guild has set none.
     */
    private @NotNull Optional<ChatColor.Legacy> tagColor = Optional.empty();

    /**
     * Total guild experience, which the guild's level is derived from.
     */
    @SerializedName("exp")
    private long experience;

    /**
     * Every member currently in the guild.
     */
    private @NotNull ConcurrentList<Member> members = Concurrent.newList();

    /**
     * Ranks the guild declares for its members. The guild master rank is not among them, which is
     * what makes a member's unmatched rank name identify the guild master.
     */
    private @NotNull ConcurrentList<Rank> ranks = Concurrent.newList();

    /**
     * Guild achievement progress, keyed by achievement name.
     */
    private @NotNull ConcurrentMap<String, Integer> achievements = Concurrent.newMap();

    /**
     * Games the guild lists as its preferred games.
     */
    private @NotNull ConcurrentList<String> preferredGames = Concurrent.newList();

    /**
     * Guild experience earned per game, keyed by the game's type name.
     */
    @SerializedName("guildExpByGameType")
    private @NotNull ConcurrentMap<String, Long> experienceByGameType = Concurrent.newMap();

    /**
     * The guild master, found as the one member whose rank name matches none of the guild's declared
     * ranks.
     */
    public @NotNull Member getGuildMaster() {
        return this.getMembers()
            .stream()
            .filter(Member::isGuildMaster)
            .findFirst()
            .orElseThrow(); // Will Never Throw
    }

    /**
     * Guild level derived from total experience by spending each level's cost in turn, the last
     * declared cost repeating for every level beyond the table.
     */
    public int getLevel() {
        int level = 0;
        long experience = this.getExperience();

        for (int i = 0; ; i++) {
            int next = i >= HYPIXEL_GUILD_EXP.size() ? HYPIXEL_GUILD_EXP.findLast().orElse(0) : HYPIXEL_GUILD_EXP.get(i);
            experience -= next;

            if (experience < 0)
                return level;
            else
                level++;
        }
    }

    /**
     * One player's membership of the guild.
     */
    @Getter
    public class Member {

        /**
         * Unique id of the player.
         */
        @SerializedName("uuid")
        private UUID uniqueId;

        /**
         * Name of the rank the member holds, matched by name against the guild's declared ranks.
         */
        @Getter(AccessLevel.NONE)
        private String rank;

        /**
         * When the member joined the guild.
         */
        private Instant joined;

        /**
         * Number of guild quests the member has taken part in.
         */
        private int questParticipation;

        /**
         * Guild experience the member earned on each recent day, keyed by date.
         */
        @SerializedName("expHistory")
        private Map<String, Integer> experienceHistory;

        /**
         * The guild rank matching this member's rank name, falling back to a guild master rank built
         * on the spot because the guild never declares one.
         */
        public @NotNull Rank getRank() {
            return HypixelGuild.this.ranks.stream()
                .filter(rank -> rank.getName().equals(this.rank))
                .findFirst()
                .orElse(Rank.buildGM(HypixelGuild.this.created));
        }

        /**
         * Whether the member's rank name matches none of the guild's declared ranks, which is the
         * only marker the guild master carries.
         */
        private boolean isGuildMaster() {
            return HypixelGuild.this.ranks.stream().noneMatch(rank -> this.rank.equals(rank.getName())); // Pigicial Jank
        }

    }

    /**
     * One rank a guild declares for its members.
     */
    @Getter
    public static class Rank {

        /**
         * Name of the rank, which is what a member's rank name is matched against.
         */
        private String name;

        /**
         * Short tag rendered for members holding the rank.
         */
        private String tag;

        /**
         * When the rank was created.
         */
        private Instant created;

        /**
         * Sort priority of the rank, higher meaning further up the guild's ladder.
         */
        private int priority;

        /**
         * Whether a newly joined member is given this rank.
         */
        @SerializedName("default")
        private boolean isDefault;

        /**
         * Builds the guild master rank, which no guild declares alongside its other ranks.
         *
         * @param created when the guild itself was created, carried onto the rank
         * @return the guild master rank
         */
        private static @NotNull Rank buildGM(Instant created) {
            Rank rank = new Rank();
            rank.name = "Guild Master";
            rank.tag = "GM";
            rank.created = created;
            rank.priority = 10;
            rank.isDefault = false;
            return rank;
        }

    }

}