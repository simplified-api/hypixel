package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.foraging;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.Extract;
import dev.sbs.api.io.gson.Lenient;
import dev.sbs.api.io.gson.SerializedPath;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
public class Foraging {

    @SerializedPath("starlyn.personal_bests")
    private @NotNull ConcurrentMap<String, Integer> personalBests = Concurrent.newMap();
    @SerializedName("fish_family")
    private @NotNull ConcurrentList<String> fishFamily = Concurrent.newList();

    // Tree Gifts
    @Lenient
    @SerializedName("tree_gifts")
    private @NotNull ConcurrentMap<String, Integer> treeGifts = Concurrent.newMap();
    @Extract("treeGifts.milestone_tier_claimed")
    private @NotNull ConcurrentMap<String, Integer> claimedMilestoneTiers = Concurrent.newMap();

    // Hina
    @SerializedPath("hina.tasks")
    private @NotNull Hina hina = new Hina();

    // Melody Harp
    @SerializedPath("songs.harp")
    private @NotNull MelodyHarp melodyHarp = new MelodyHarp();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Hina {

        @SerializedName("completed_tasks")
        private @NotNull ConcurrentList<String> completedTasks = Concurrent.newList();
        @SerializedName("task_progress")
        private @NotNull ConcurrentMap<String, Integer> taskProgress = Concurrent.newMap();
        @SerializedName("claimed_rewards")
        private @NotNull ConcurrentList<String> claimedRewards = Concurrent.newList();
        @SerializedName("tier_claimed")
        private int tierClaimed;

    }

}
