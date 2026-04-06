package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Getter
public class TimecharmGallery {

    @SerializedName("elise_step")
    private int eliseStep;
    @SerializedName("secured_trophies")
    private @NotNull ConcurrentList<Trophy> securedTrophies = Concurrent.newList();
    @SerializedName("sent_trophy_dialogues")
    private @NotNull ConcurrentList<String> sentTrophyDialogues = Concurrent.newList();

    @Getter
    public static class Trophy {

        private String type;
        private Instant timestamp;
        private int visits;

    }

}
