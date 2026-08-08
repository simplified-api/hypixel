package api.simplified.hypixel.response.skyblock.member;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class Trapper {

    @SerializedName("last_task_time")
    private SkyBlockDate.RealTime lastTask;
    @SerializedName("pelt_count")
    private int peltCount;

}
