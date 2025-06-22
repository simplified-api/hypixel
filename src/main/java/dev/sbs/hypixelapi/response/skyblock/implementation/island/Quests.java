package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.linked.ConcurrentLinkedMap;
import dev.sbs.api.stream.pair.Pair;
import dev.sbs.api.util.NumberUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

public class Quests {

    @SerializedName("harp_quest")
    private @NotNull ConcurrentMap<String, Object> melodyHarpMap = Concurrent.newMap();
    private MelodyHarp melodyHarp;
    @SerializedName("trapper_quest")
    @Getter private Trapper trapper = new Trapper();

    public @NotNull MelodyHarp getMelodyHarp() {
        if (this.melodyHarp == null)
            this.melodyHarp = new MelodyHarp(this.melodyHarpMap);

        return this.melodyHarp;
    }

    @Getter
    public static class MelodyHarp {

        private final boolean talismanClaimed;
        private final String selectedSong;
        private final SkyBlockDate.RealTime selectedSongTimestamp;
        private final ConcurrentMap<String, Song> songs;

        MelodyHarp(@NotNull ConcurrentMap<String, Object> harpQuest) {
            this.talismanClaimed = (boolean) harpQuest.removeOrGet("claimed_talisman", false);
            this.selectedSong = (String) harpQuest.removeOrGet("selected_song", "");
            long epoch = NumberUtil.createNumber(String.valueOf(harpQuest.removeOrGet("selected_song_epoch", 0))).longValue();
            this.selectedSongTimestamp = new SkyBlockDate.RealTime(epoch * 1000) ;

            ConcurrentLinkedMap<String, ConcurrentMap<String, Integer>> songMap = Concurrent.newLinkedMap();
            harpQuest.forEach((harpKey, harpValue) -> {
                if (harpValue instanceof Number) {
                    String songKey = harpKey.replace("song_", "");
                    String songName = songKey.replaceAll("_((best|perfect)_)?completions?", "");
                    String category = songKey.replace(String.format("%s_", songName), "");

                    if (!songMap.containsKey(songName))
                        songMap.put(songName, Concurrent.newMap());

                    songMap.get(songName).put(category, NumberUtil.createNumber(harpValue.toString()).intValue());
                }
            });

            this.songs = Concurrent.newUnmodifiableMap(
                songMap.stream()
                    .map(entry -> Pair.of(
                        entry.getKey(),
                        new Song(
                            entry.getValue().getOrDefault("best_completion", 0),
                            entry.getValue().getOrDefault("completions", 0),
                            entry.getValue().getOrDefault("perfect_completions", 0)
                        )
                    ))
                    .collect(Concurrent.toMap())
            );
        }

        @Getter
        @RequiredArgsConstructor
        public static class Song {

            private final int bestCompletion;
            private final int completions;
            private final int perfectCompletions;

        }

    }

    @Getter
    public static class Trapper {

        @SerializedName("last_task_time")
        private SkyBlockDate.RealTime lastTask;
        @SerializedName("pelt_count")
        private int peltCount;

    }

}
