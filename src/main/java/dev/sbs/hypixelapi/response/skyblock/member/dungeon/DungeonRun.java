package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.dungeon;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
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
        private String displayName;
        @SerializedName("class_milestone")
        private int milestone;

        public int getClassLevel() {
            return Integer.parseInt(DISPLAY_PATTERN.matcher(this.getDisplayName()).group(4));
        }

        public @NotNull DungeonClass.Type getClassType() {
            return DungeonClass.Type.of(DISPLAY_PATTERN.matcher(this.getDisplayName()).group(3).toUpperCase());
        }

        public @NotNull String getName() {
            return DISPLAY_PATTERN.matcher(this.getDisplayName()).group(2);
        }

    }

}
