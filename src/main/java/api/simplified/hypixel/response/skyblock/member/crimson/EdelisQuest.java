package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * A member's progress through Edelis' quest, which the wire records as a single flag.
 * <p>
 * The class exists only because that flag gets its own {@code edelis_quest} object rather than sitting
 * beside the other booleans on the quests node. There is no timestamp and nothing else.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Edelis">Edelis</a>
 */
@Getter
@NoArgsConstructor
public class EdelisQuest {

    /**
     * Whether the member has heard the story told at the statue - fluent, so the accessor reads
     * {@code hasHeardStoryStatue()}.
     */
    @Accessors(fluent = true)
    @SerializedName("heard_story_statue")
    private boolean hasHeardStoryStatue;

}
