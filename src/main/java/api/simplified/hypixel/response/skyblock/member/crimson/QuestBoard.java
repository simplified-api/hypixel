package api.simplified.hypixel.response.skyblock.member.crimson;

import api.simplified.hypixel.response.skyblock.member.Objective;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor
public class QuestBoard {

    @SerializedName("quest_list")
    private @NotNull ConcurrentList<String> questList = Concurrent.newList();

    @SerializedName("boss")
    private @NotNull Objective bossQuest = new Objective();
    @SerializedName("dojo")
    private @NotNull Objective dojoQuest = new Objective();
    @SerializedName("fetch")
    private @NotNull Objective fetchQuest = new Objective();
    @SerializedName("fishing")
    private @NotNull Objective fishingQuest = new Objective();
    @SerializedName("wanted_mini_boss")
    private @NotNull Objective miniBossQuest = new Objective();

}
