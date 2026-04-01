package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.Capture;
import dev.sbs.api.io.gson.Split;
import dev.sbs.api.tuple.pair.PairOptional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
public class TrophyFishing {

    @SerializedName("total_caught")
    private int totalCaught;
    @SerializedName("rewards")
    private @NotNull ConcurrentList<Integer> rewards = Concurrent.newList();
    @Split("/")
    @SerializedName("last_caught")
    private @NotNull PairOptional<TrophyFish, TrophyFish.Tier> lastCaught = PairOptional.empty();
    @Capture
    private @NotNull ConcurrentMap<TrophyFish, TierData> fish = Concurrent.newMap();

    @Getter
    @NoArgsConstructor
    public static class TierData {

        @SerializedName("")
        private int total;
        private int bronze;
        private int silver;
        private int gold;
        private int diamond;

    }

}
