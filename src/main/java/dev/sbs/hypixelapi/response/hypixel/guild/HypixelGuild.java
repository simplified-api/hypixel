package dev.sbs.minecraftapi.client.hypixel.response.hypixel.guild;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Getter
public class HypixelGuild {

    private final static ConcurrentList<Integer> HYPIXEL_GUILD_EXP = Concurrent.newList(
        100_000, 150_000, 250_000, 500_000, 750_000, 1_000_000, 1_250_000, 1_500_000,
        2_000_000, 2_500_000, 2_500_000, 2_500_000, 2_500_000, 2_500_000, 3_000_000
    );

    @SerializedName("_id")
    private String guildId;
    private String name;
    private @NotNull Optional<String> tag = Optional.empty();
    private @NotNull Optional<String> description = Optional.empty();
    private long chatMute;
    private int coins;
    private int coinsEver;
    private Instant created;
    private boolean publiclyListed;
    private @NotNull Optional<ChatFormat> tagColor = Optional.empty();
    @SerializedName("exp")
    private long experience;
    private @NotNull ConcurrentList<Member> members = Concurrent.newList();
    private @NotNull ConcurrentList<Rank> ranks = Concurrent.newList();
    private @NotNull ConcurrentMap<String, Integer> achievements = Concurrent.newMap();
    private @NotNull ConcurrentList<String> preferredGames = Concurrent.newList();
    @SerializedName("guildExpByGameType")
    private @NotNull ConcurrentMap<String, Long> experienceByGameType = Concurrent.newMap();

    public @NotNull Member getGuildMaster() {
        return this.getMembers()
            .stream()
            .filter(Member::isGuildMaster)
            .findFirst()
            .orElseThrow(); // Will Never Throw
    }

    public int getLevel() {
        int level = 0;
        long experience = this.getExperience();

        for (int i = 0; ; i++) {
            int next = i >= HYPIXEL_GUILD_EXP.size() ? HYPIXEL_GUILD_EXP.getLast().orElse(0) : HYPIXEL_GUILD_EXP.get(i);
            experience -= next;

            if (experience < 0)
                return level;
            else
                level++;
        }
    }

    @Getter
    public class Member {

        @SerializedName("uuid")
        private UUID uniqueId;
        @Getter(AccessLevel.NONE)
        private String rank;
        private Instant joined;
        private int questParticipation;
        @SerializedName("expHistory")
        private Map<String, Integer> experienceHistory;

        public @NotNull Rank getRank() {
            if (this.isGuildMaster())
                return Rank.buildGM(HypixelGuild.this.created);
            else
                return HypixelGuild.this.ranks.stream()
                    .filter(rank -> rank.getName().equals(this.rank))
                    .findFirst()
                    .orElseThrow();
        }

        private boolean isGuildMaster() {
            return HypixelGuild.this.ranks.stream().noneMatch(rank -> this.rank.equals(rank.getName())); // Pigicial Jank
        }

    }

    @Getter
    public static class Rank {

        private String name;
        private String tag;
        private Instant created;
        private int priority;
        @SerializedName("default")
        private boolean isDefault;

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