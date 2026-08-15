package api.simplified.hypixel.response.skyblock.member.crimson;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * One Crimson Isle NPC's quest, in the single shape four different NPCs bind to.
 * <p>
 * Four wire nodes decode here and they differ only in which of the four keys they happen to send: some
 * record a completion as a timestamp, one records it as a flag, and only one tracks a toy drop.
 * Splitting them into a class each would give near-identical files whose only difference is a field one
 * of them omits.
 * <p>
 * A key an NPC does not send stays absent rather than binding a default, so an unsent timestamp reads
 * as an empty {@link Optional} instead of a fabricated one.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/NPC">NPC</a>
 */
@Getter
@NoArgsConstructor
public class NpcQuest {

    /**
     * Whether the member has struck up the conversation that starts the quest. It is cleared again when
     * the NPC resets its dialogue for the next cycle, so it can read false beside a recorded
     * completion.
     */
    @SerializedName("talked_to_npc")
    private boolean talkedToNpc;

    /**
     * Whether a one-shot quest is done, recorded as a flag because there is nothing to time.
     */
    @SerializedName("completed_quest")
    private boolean completedQuest;

    /**
     * A repeatable quest's most recent turn-in, recorded as a timestamp rather than a flag, and empty
     * on an NPC whose quest does not repeat.
     */
    @SerializedName("last_completion")
    private @NotNull Optional<SkyBlockDate.RealTime> lastCompletion = Optional.empty();

    /**
     * When the NPC last dropped the toy its quest revolves around, tracked separately from the turn-in
     * and empty on every NPC that does not send it.
     */
    @SerializedName("last_toy_drop")
    private @NotNull Optional<SkyBlockDate.RealTime> lastToyDrop = Optional.empty();

}
