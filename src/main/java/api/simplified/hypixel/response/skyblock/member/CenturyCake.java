package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.date.SkyBlockDate;
import lombok.Getter;

@Getter
public class CenturyCake {

    private int stat; // This is in ordinal order in stat menu
    private String key;
    private int amount;
    @SerializedName("expire_at")
    private SkyBlockDate.RealTime expiresAt;

}
