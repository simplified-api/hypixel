package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;

import java.time.Instant;

public class ChocolateTimeTower {

    private int charges;
    @SerializedName("activation_time")
    private Instant activationTime;
    private int level;
    @SerializedName("last_charge_time")
    private Instant lastChargeTime;

}
