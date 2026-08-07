package api.simplified.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.time.Instant;

@Getter
public class ChocolateTimeTower {

    private int charges;
    @SerializedName("activation_time")
    private Instant activationTime;
    private int level;
    @SerializedName("last_charge_time")
    private Instant lastChargeTime;

}
