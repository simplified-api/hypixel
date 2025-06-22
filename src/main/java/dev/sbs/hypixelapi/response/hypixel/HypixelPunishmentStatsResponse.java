package dev.sbs.minecraftapi.client.hypixel.response.hypixel;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class HypixelPunishmentStatsResponse {

    private boolean success;

    // Watchdog
    @SerializedName("watchdog_lastMinute")
    private int watchdogLastMinute;
    @SerializedName("watchdog_total")
    private int watchdogTotal;
    @SerializedName("watchdog_rollingDaily")
    private int watchdogRollingDaily;

    // Staff
    @SerializedName("staff_rollingDaily")
    private int staffRollingDaily;
    @SerializedName("staff_total")
    private int staffTotal;

}
