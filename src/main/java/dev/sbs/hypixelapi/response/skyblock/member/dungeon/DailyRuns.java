package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.dungeon;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class DailyRuns {

    @SerializedName("current_day_stamp")
    private int currentDayStamp;
    @SerializedName("completed_runs_count")
    private int completedRuns;

}
