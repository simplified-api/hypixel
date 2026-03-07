package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
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
    private @NotNull Cowboy cowboy = new Cowboy();
    private @NotNull Lonely lonely = new Lonely();
    private @NotNull Seraphine seraphine = new Seraphine();

    @Getter
    public static class Murder {

        @SerializedName("step_index")
        private int stepIndex;
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

    @Getter
    public static class Lonely {

        @SerializedName("seconds_sitting")
        private int secondsSitting;

    }

    @Getter
    public static class Seraphine {

        @SerializedName("step_index")
        private int stepIndex;

    }

}
