package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * A member's progress through the hunt for the Crimson Isle's named chickens.
 * <p>
 * As on the alchemist's quest, the wire prefixes every key with {@code chicken_quest_} inside an
 * object already called {@code chicken_quest} and the Java names drop the repetition. The turn-in
 * timestamp is not here - it sits one level up on the quests node.
 */
@Getter
@NoArgsConstructor
public class ChickenQuest {

    /**
     * Whether the member has begun the hunt.
     */
    @SerializedName("chicken_quest_start")
    private boolean started;

    /**
     * The hunt's step counter, which does not count the chickens found - it runs behind the collected
     * list and nothing here says what advances it.
     */
    @SerializedName("chicken_quest_progress")
    private int progress;

    /**
     * The named chickens the member has found so far, spelled as display names rather than item ids,
     * so they resolve against no repository.
     */
    @SerializedName("chicken_quest_collected")
    private @NotNull ConcurrentList<String> collected = Concurrent.newList();

}
