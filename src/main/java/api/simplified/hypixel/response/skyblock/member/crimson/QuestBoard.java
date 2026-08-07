package api.simplified.hypixel.response.skyblock.member.crimson;

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
    private @NotNull BoardQuest bossQuest = new BoardQuest();
    @SerializedName("dojo")
    private @NotNull BoardQuest dojoQuest = new BoardQuest();
    @SerializedName("fetch")
    private @NotNull BoardQuest fetchQuest = new BoardQuest();
    @SerializedName("fishing")
    private @NotNull BoardQuest fishingQuest = new BoardQuest();
    @SerializedName("wanted_mini_boss")
    private @NotNull BoardQuest miniBossQuest = new BoardQuest();

}
