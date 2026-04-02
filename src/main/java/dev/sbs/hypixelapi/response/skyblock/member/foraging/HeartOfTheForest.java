package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.foraging;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.Getter;
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

}
