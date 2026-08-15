package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NamingStyle;
import dev.simplified.annotations.NoArgsConstructor;

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
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("heard_story_statue")
    private boolean hasHeardStoryStatue;

}
