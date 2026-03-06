package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.linked.ConcurrentLinkedMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class Quests implements PostInit {

    @Getter(AccessLevel.NONE)
    @SerializedName("harp_quest")
    private @NotNull ConcurrentMap<String, Object> melodyHarpMap = Concurrent.newMap();
    private transient MelodyHarp melodyHarp;
    @SerializedName("trapper_quest")
    private @NotNull Trapper trapper = new Trapper();

    @Override
    public void postInit() {
        this.melodyHarp = new MelodyHarp(this.melodyHarpMap);
    }

    @Getter
    public static class MelodyHarp {

        private final boolean talismanClaimed;
        private final @NotNull Optional<String> selectedSong;
        private final @NotNull SkyBlockDate.RealTime selectedSongTimestamp;
        private final @NotNull ConcurrentMap<String, MelodyHarp.Song> songs;

        MelodyHarp(@NotNull ConcurrentMap<String, Object> harpQuest) {
            this.talismanClaimed = (boolean) harpQuest.removeOrGet("claimed_talisman", false);
            this.selectedSong = harpQuest.getOptional("selected_song").map(String::valueOf);
            long epoch = NumberUtil.createNumber(String.valueOf(harpQuest.removeOrGet("selected_song_epoch", 0))).longValue();
            this.selectedSongTimestamp = new SkyBlockDate.RealTime(epoch * 1000);

            ConcurrentLinkedMap<String, ConcurrentMap<String, Integer>> songMap = Concurrent.newLinkedMap();
            harpQuest.stream()
                .filterValue(Number.class::isInstance)
                .forEach((key, value) -> {
                    String songKey = key.replace("song_", "");
                    String songName = songKey.replaceAll("_((best|perfect)_)?completions?", "");
                    String category = songKey.replace(String.format("%s_", songName), "");

                    if (!songMap.containsKey(songName))
                        songMap.put(songName, Concurrent.newMap());

                    songMap.get(songName).put(category, NumberUtil.createNumber(value.toString()).intValue());
                });

            this.songs = songMap.stream()
                .map((key, value) -> Pair.of(
                    key,
                    new MelodyHarp.Song(
                        value.getOrDefault("best_completion", 0),
                        value.getOrDefault("completions", 0),
                        value.getOrDefault("perfect_completions", 0)
                    )
                ))
                .collect(Concurrent.toUnmodifiableMap());
        }

        @Getter
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Song {

            private final int bestCompletion;
            private final int completions;
            private final int perfectCompletions;

        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Trapper {

        @SerializedName("last_task_time")
        private SkyBlockDate.RealTime lastTask;
        @SerializedName("pelt_count")
        private int peltCount;

    }

}
