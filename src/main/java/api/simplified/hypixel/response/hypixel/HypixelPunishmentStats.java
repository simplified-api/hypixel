package api.simplified.hypixel.response.hypixel;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

/**
 * Network-wide punishment counts, split between the automated Watchdog anticheat and the staff team.
 */
@Getter
public class HypixelPunishmentStats {

    /**
     * Whether the wire reported the request as successful.
     */
    private boolean success;

    // Watchdog

    /**
     * Bans Watchdog issued in the last minute.
     */
    @SerializedName("watchdog_lastMinute")
    private int watchdogLastMinute;

    /**
     * Bans Watchdog has issued over the network's lifetime.
     */
    @SerializedName("watchdog_total")
    private int watchdogTotal;

    /**
     * Bans Watchdog issued over the trailing day.
     */
    @SerializedName("watchdog_rollingDaily")
    private int watchdogRollingDaily;

    // Staff

    /**
     * Bans staff issued over the trailing day.
     */
    @SerializedName("staff_rollingDaily")
    private int staffRollingDaily;

    /**
     * Bans staff have issued over the network's lifetime.
     */
    @SerializedName("staff_total")
    private int staffTotal;

}
