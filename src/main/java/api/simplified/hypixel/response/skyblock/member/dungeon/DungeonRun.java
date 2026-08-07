package api.simplified.hypixel.response.skyblock.member.dungeon;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.util.NumberUtil;
import lib.minecraft.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class DungeonRun {

    @SerializedName("run_id")
    private @NotNull UUID id;
    @SerializedName("completion_ts")
    private SkyBlockDate.RealTime completionTime;
    @SerializedName("dungeon_type")
    private @NotNull DungeonData.Type dungeonType = DungeonData.Type.UNKNOWN;
    @SerializedName("dungeon_tier")
    private int tier;
    private @NotNull ConcurrentList<Participant> participants = Concurrent.newList();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Participant {

        private static final Pattern DISPLAY_PATTERN = Pattern.compile(String.format(
            "^%s([0-9a-f])(.*?)%<s[0-9a-f]: %<s[0-9a-f](.*?)%<s[0-9a-f] \\(%<s[0-9a-f]([0-9]+)%<s[0-9a-f]\\)",
            ChatFormat.SECTION_SYMBOL
        ));

        @SerializedName("player_uuid")
        private UUID playerId;
        @SerializedName("display_name")
        private @NotNull String displayName = "";
        @SerializedName("class_milestone")
        private int milestone;

        public int getClassLevel() {
            return this.matchDisplayName()
                .map(matcher -> NumberUtil.tryParseInt(matcher.group(4)))
                .orElse(0);
        }

        public @NotNull DungeonClass.Type getClassType() {
            return this.matchDisplayName()
                .map(matcher -> DungeonClass.Type.of(matcher.group(3)))
                .orElse(DungeonClass.Type.UNKNOWN);
        }

        public @NotNull String getName() {
            return this.matchDisplayName()
                .map(matcher -> matcher.group(2))
                .orElse(this.getDisplayName());
        }

        /**
         * Matches the chat-formatted display name once, so the three accessors read their groups off a
         * matcher that has matched rather than off a fresh one.
         *
         * @return the matched display name, or empty when it does not carry the expected shape
         */
        private @NotNull Optional<Matcher> matchDisplayName() {
            Matcher matcher = DISPLAY_PATTERN.matcher(this.getDisplayName());
            return matcher.find() ? Optional.of(matcher) : Optional.empty();
        }

    }

}
