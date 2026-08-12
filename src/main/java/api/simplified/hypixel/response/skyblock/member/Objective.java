package api.simplified.hypixel.response.skyblock.member;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A tracked objective, shared by the Crimson Isle quest board and the member's own objective list.
 *
 * <p>
 * Every field is bound; nothing here derives and nothing reaches a repository. On the quest board
 * each slot is one of these, non-null by default.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Quests">Quests</a>
 */
@Getter
@NoArgsConstructor
public class Objective {

    /**
     * Where the objective stands.
     */
    private @NotNull Status status = Status.UNKNOWN;

    /**
     * A counter the objective defines for itself, with no meaning shared across objectives.
     */
    private int progress;

    /**
     * When the objective was finished. An unfinished objective carries a literal zero rather than
     * omitting the key, so empty here means the key was absent altogether.
     */
    @SerializedName("completed_at")
    private @NotNull Optional<SkyBlockDate.RealTime> completedAt = Optional.empty();

    /**
     * The completion count the wire reports for a repeatable objective.
     */
    private @NotNull Optional<Integer> completions = Optional.empty();

    /**
     * Per-objective progress, keyed however the objective spells it - a collected item id, a
     * talked-to npc, a required material, a bare index.
     *
     * <p>
     * Every key no other field claims is folded in here, so there is no schema to it. Two shapes are
     * worth knowing: a collection id carries colons, and {@code INK_SACK:3} is a different collection
     * from {@code INK_SACK}; and a requirement the wire spells as a boolean does not bind against the
     * declared integer value, so it falls into overflow and leaves nothing behind.
     */
    @Capture
    private @NotNull ConcurrentMap<String, Integer> requirements = Concurrent.newMap();

    /**
     * The states an objective can be in.
     */
    public enum Status {

        /**
         * The default, carried when the wire named no status at all. It is never a value the wire
         * itself sends.
         */
        UNKNOWN,

        /**
         * Visible but not being worked on.
         */
        INACTIVE,

        /**
         * Currently being worked on.
         */
        ACTIVE,

        /**
         * Finished. The wire spells this {@code COMPLETE} and never {@code COMPLETED}, and the
         * longer spelling would bind every finished objective onto null.
         */
        COMPLETE

    }

}
