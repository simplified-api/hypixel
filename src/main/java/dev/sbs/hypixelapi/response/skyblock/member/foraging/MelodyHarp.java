package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.foraging;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.Capture;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class MelodyHarp {

    @SerializedName("claimed_talisman")
    private boolean talismanClaimed;
    @SerializedName("selected_song")
    private @NotNull Optional<String> selectedSong = Optional.empty();
    @SerializedName("selected_song_epoch")
    private @NotNull SkyBlockDate.RealTime selectedSongTimestamp = new SkyBlockDate.RealTime(0);
    @Capture(filter = "^song_")
    private @NotNull ConcurrentMap<String, Song> songs = Concurrent.newMap();

    @Getter
    @NoArgsConstructor
    public static class Song {

        @SerializedName("best_completion")
        private double bestCompletion;
        private int completions;
        @SerializedName("perfect_completions")
        private int perfectCompletions;

    }

}
