package api.simplified.hypixel.response.skyblock.member.dungeon;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.util.NumberUtil;
import lib.minecraft.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One completed dungeon run whose reward chests have not been claimed yet.
 * <p>
 * Croesus in the Dungeon Hub holds these - up to 60 runs, each expiring after 72 hours - so the list
 * is the member's outstanding loot rather than a run history.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Croesus">Croesus</a>
 */
@Getter
public class DungeonRun {

    /**
     * Uuid identifying the run, the key a {@link DungeonChest} points back at.
     */
    @SerializedName("run_id")
    private @NotNull UUID id;

    /**
     * When the run finished, in real time. The 72-hour expiry counts from here.
     * <p>
     * Carries no default, so it is null when the wire omits it.
     */
    @SerializedName("completion_ts")
    private SkyBlockDate.RealTime completionTime;

    /**
     * Which dungeon the run was in, the Catacombs being the only one the type table names.
     */
    @SerializedName("dungeon_type")
    private @NotNull DungeonData.Type dungeonType = DungeonData.Type.UNKNOWN;

    /**
     * The floor number the run was on, {@code 0} through {@code 7}.
     * <p>
     * An {@code int} on purpose - the wire sends a bare number, and {@link Floor#of(int)} converts
     * it without throwing on anything out of range.
     */
    @SerializedName("dungeon_tier")
    private int tier;

    /**
     * The party that ran it, the member included.
     */
    private @NotNull ConcurrentList<Participant> participants = Concurrent.newList();

    /**
     * One player in a stored run, as Croesus displays them.
     * <p>
     * The name, the class and the class level all arrive pre-rendered in a single chat-formatted
     * line, so each of them is derived by re-reading that line rather than bound from a wire key of
     * its own.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Participant {

        /**
         * Shape of the chat-formatted display line, carrying the player name, the class and the
         * class level between colour codes.
         */
        private static final Pattern DISPLAY_PATTERN = Pattern.compile(String.format(
            "^%s([0-9a-f])(.*?)%<s[0-9a-f]: %<s[0-9a-f](.*?)%<s[0-9a-f] \\(%<s[0-9a-f]([0-9]+)%<s[0-9a-f]\\)",
            ChatFormat.SECTION_SYMBOL
        ));

        /**
         * That player's uuid.
         */
        @SerializedName("player_uuid")
        private UUID playerId;

        /**
         * The party line as the game renders it, colour codes and all, empty when the wire omits it.
         */
        @SerializedName("display_name")
        private @NotNull String displayName = "";

        /**
         * The class milestone that player reached in the run, {@code 0} to {@code 9}.
         * <p>
         * Milestone 2 is what lets a reward chest be opened and milestone 3 what avoids a reduced
         * Dungeoneering payout. Bound from the wire, unlike the class level, which is derived.
         */
        @SerializedName("class_milestone")
        private int milestone;

        /**
         * The class level the display line shows for that player, zero when the line does not carry
         * the expected shape.
         * <p>
         * Derived on every call and not the same number as the bound milestone.
         */
        public int getClassLevel() {
            return this.matchDisplayName()
                .map(matcher -> NumberUtil.tryParseInt(matcher.group(4)))
                .orElse(0);
        }

        /**
         * The class the display line names, {@link DungeonClass.Type#UNKNOWN} when the line does
         * not carry the expected shape.
         */
        public @NotNull DungeonClass.Type getClassType() {
            return this.matchDisplayName()
                .flatMap(matcher -> DungeonClass.Type.findByName(matcher.group(3)))
                .orElse(DungeonClass.Type.UNKNOWN);
        }

        /**
         * The player's name out of the display line, falling back to the whole line when it does
         * not carry the expected shape.
         */
        public @NotNull String getName() {
            return this.matchDisplayName()
                .map(matcher -> matcher.group(2))
                .orElse(this.getDisplayName());
        }

        /**
         * Matches the display name against the shape the game renders it in.
         * <p>
         * Every accessor above funnels through here, because reading a group off a matcher that
         * never matched throws rather than answering with nothing. The pattern requires the colour
         * codes, so a plain name matches nothing and each caller takes its own fallback.
         *
         * @return the matcher positioned on the display name, or empty when it does not carry the
         * expected shape
         */
        private @NotNull Optional<Matcher> matchDisplayName() {
            Matcher matcher = DISPLAY_PATTERN.matcher(this.getDisplayName());
            return matcher.find() ? Optional.of(matcher) : Optional.empty();
        }

    }

}
