package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.PairOptional;
import dev.simplified.gson.Capture;
import dev.simplified.gson.Split;
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
