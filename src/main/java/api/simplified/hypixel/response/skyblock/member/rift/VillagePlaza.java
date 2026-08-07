package api.simplified.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.SerializedPath;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Getter
public class VillagePlaza {

    @Accessors(fluent = true)
    @SerializedName("got_scammed")
    private boolean hasBeenScammed;
    private @NotNull Murder murder = new Murder();
    @SerializedName("barry_center")
    private @NotNull BarryCenter barryCenter = new BarryCenter();
    @SerializedName("barter_bank")
    private @NotNull ConcurrentMap<String, Object> barterBank = Concurrent.newMap();
    private @NotNull Cowboy cowboy = new Cowboy();
    @SerializedPath("lonely.seconds_sitting")
    private int secondsSitting;
    @SerializedPath("seraphine.step_index")
    private int seraphineStepIndex;

    @Getter
    public static class Murder {

        @SerializedName("step_index")
        private int stepIndex;
        @SerializedName("step_index_pt2")
        private int stepIndexPt2;
        @SerializedName("step_index_pt3")
        private int stepIndexPt3;
        @SerializedName("room_clues")
        private @NotNull ConcurrentList<String> roomClues = Concurrent.newList();

    }

    @Getter
    public static class BarryCenter {

        @SerializedName("first_talk_to_barry")
        private boolean firstTalkToBarry;
        @SerializedName("received_reward")
        private boolean receivedReward;
        private @NotNull ConcurrentList<String> convinced = Concurrent.newList();

    }

    @Getter
    public static class Cowboy {

        private int stage;
        @SerializedName("hay_eaten")
        private int hayEaten;
        @SerializedName("rabbit_name")
        private String rabbitName;
        @SerializedName("exported_carrots")
        private int exportedCarrots;

    }

}
