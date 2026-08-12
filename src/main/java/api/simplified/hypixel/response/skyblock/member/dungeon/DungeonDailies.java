package api.simplified.hypixel.response.skyblock.member.dungeon;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

/**
 * A day-scoped counter of completed dungeon runs, reset when the stamped day rolls over.
 * <p>
 * A stale stamp means a stale count - the runs belong to the day that was stamped, which is not
 * necessarily today.
 */
@Getter
public class DungeonDailies {

    /**
     * The day the counter belongs to, as whole days since the Unix epoch.
     * <p>
     * Not milliseconds and not a SkyBlock date - it is a plain day count, and the only field here
     * that uses that unit.
     */
    @SerializedName("current_day_stamp")
    private int currentDayStamp;

    /**
     * Runs completed on the stamped day.
     */
    @SerializedName("completed_runs_count")
    private int completedRuns;

}
