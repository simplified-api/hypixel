package api.simplified.hypixel.response.skyblock.member.crimson;

import api.simplified.hypixel.response.skyblock.member.Objective;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * The rotating faction quests a member is currently holding.
 * <p>
 * Up to five are given a day, drawn from the fishing, fetch, miniboss, dojo and Kuudra categories, and
 * a quest id's trailing letter is its rank - {@code _s}, {@code _a}, {@code _b}, {@code _c} or
 * {@code _d} - which is what sets the reputation a turn-in pays. Every slot is an {@link Objective},
 * the same type the member's own objective list uses, so a finished quest reads {@code COMPLETE}; the
 * longer spelling appears nowhere on the wire.
 * <p>
 * Every slot observed carries a {@code completed_at} of zero. That is a present key holding zero rather
 * than an absent one, so an unfinished quest and one completed at the epoch look identical here.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Faction_Quests">Faction Quests</a>
 */
@Getter
@NoArgsConstructor
public class QuestBoard {

    /**
     * The ids of the quests currently on the board, each ending in the letter that ranks it. The same
     * ids key the reward map that says which item each quest pays.
     */
    @SerializedName("quest_list")
    private @NotNull ConcurrentList<String> questList = Concurrent.newList();

    /**
     * The Kuudra slot on the board.
     */
    @SerializedName("boss")
    private @NotNull Objective bossQuest = new Objective();

    /**
     * The Dojo slot on the board. Its quest id embeds the grade the run has to reach as well as the
     * test it names, and that grade is modelled nowhere here.
     */
    @SerializedName("dojo")
    private @NotNull Objective dojoQuest = new Objective();

    /**
     * The fetch-an-item slot on the board.
     */
    @SerializedName("fetch")
    private @NotNull Objective fetchQuest = new Objective();

    /**
     * The trophy fishing slot on the board.
     */
    @SerializedName("fishing")
    private @NotNull Objective fishingQuest = new Objective();

    /**
     * The wanted-miniboss slot on the board; the Java name drops the wire key's {@code wanted_}.
     */
    @SerializedName("wanted_mini_boss")
    private @NotNull Objective miniBossQuest = new Objective();

}
