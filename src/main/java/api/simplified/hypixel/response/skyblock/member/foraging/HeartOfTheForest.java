package api.simplified.hypixel.response.skyblock.member.foraging;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class HeartOfTheForest {

    @SerializedName("current_daily_effect")
    private Optional<String> currentLotteryEffect = Optional.empty();
    @SerializedName("current_daily_effect_last_changed")
    private int lotteryEffectLastChanged;

    // Whispers
    @SerializedName("forests_whispers")
    private int remainingForestWhispers;
    @SerializedName("forests_whispers_spent")
    private int spentForestWhispers;
    @SerializedName("whispers")
    private @NotNull ConcurrentMap<String, BiomeWhispers> biomeWhispers = Concurrent.newMap();

    // Daily Logs
    @SerializedName("daily_trees_cut")
    private int dailyTreesCut;
    @SerializedName("daily_trees_cut_day")
    private int dailyTreesCutDay;
    @SerializedName("daily_gifts")
    private int dailyGifts;
    @SerializedName("daily_log_cut")
    private @NotNull ConcurrentList<String> dailyLogCut = Concurrent.newList();
    @SerializedName("daily_log_cut_day")
    private int dailyLogCutDay;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BiomeWhispers {

        private static final @NotNull String SPENT_KEY = "spent";

        private int total;
        @Capture
        private @NotNull ConcurrentMap<Integer, ConcurrentMap<String, Integer>> tiers = Concurrent.newMap();

        public int getSpent(int tier) {
            return this.getTiers()
                .getOrDefault(tier, Concurrent.newUnmodifiableMap())
                .getOrDefault(SPENT_KEY, 0);
        }

    }

}
