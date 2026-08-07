package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A tracked objective, shared by the crimson isle quest board and the member's own objective list.
 */
@Getter
@NoArgsConstructor
public class Objective {

    private @NotNull Status status = Status.UNKNOWN;
    private int progress;
    @SerializedName("completed_at")
    private @NotNull Optional<SkyBlockDate.RealTime> completedAt = Optional.empty();
    private @NotNull Optional<Integer> completions = Optional.empty();

    /**
     * Per-objective progress, keyed however the objective spells it - a collected item id, a
     * talked-to npc, a required material
     */
    @Capture
    private @NotNull ConcurrentMap<String, Integer> requirements = Concurrent.newMap();

    public enum Status {

        UNKNOWN,
        INACTIVE,
        ACTIVE,
        COMPLETE

    }

}
