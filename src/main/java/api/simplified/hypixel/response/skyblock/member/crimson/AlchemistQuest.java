package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A member's progress through the Crimson Isle alchemist's multi-step quest.
 * <p>
 * The wire prefixes both keys with {@code alchemist_quest_} inside an object already called
 * {@code alchemist_quest}, so the full path to the counter is
 * {@code quests.alchemist_quest.alchemist_quest_progress}. The Java names drop the repetition.
 */
@Getter
@NoArgsConstructor
public class AlchemistQuest {

    /**
     * Whether the member has begun the alchemist's quest.
     */
    @SerializedName("alchemist_quest_start")
    private boolean started;

    /**
     * How many steps into the quest the member has got, with nothing here bounding the count.
     */
    @SerializedName("alchemist_quest_progress")
    private int progress;

}
