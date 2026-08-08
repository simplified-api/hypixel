package api.simplified.hypixel.response.skyblock.member;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class CenturyCake {

    private int stat; // This is in ordinal order in stat menu
    private String key;
    private int amount;
    @SerializedName("expire_at")
    private SkyBlockDate.RealTime expiresAt;

}
