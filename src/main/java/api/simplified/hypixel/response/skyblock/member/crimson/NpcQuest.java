package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.date.SkyBlockDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A crimson isle npc quest.
 * <p>
 * One shape covers every npc: they differ only in whether the wire records completion as a timestamp
 * or as a flag, and whether a toy drop is tracked. A key the npc does not carry stays absent rather
 * than binding to a default.
 */
@Getter
@NoArgsConstructor
public class NpcQuest {

    @SerializedName("talked_to_npc")
    private boolean talkedToNpc;
    @SerializedName("completed_quest")
    private boolean completedQuest;
    @SerializedName("last_completion")
    private @NotNull Optional<SkyBlockDate.RealTime> lastCompletion = Optional.empty();
    @SerializedName("last_toy_drop")
    private @NotNull Optional<SkyBlockDate.RealTime> lastToyDrop = Optional.empty();

}
