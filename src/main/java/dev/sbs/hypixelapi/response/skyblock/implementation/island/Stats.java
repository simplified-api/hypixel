package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class Stats {

    @SerializedName("highest_crit_damage")
    private double highestCritDamage;
    @SerializedName("glowing_mushrooms_broken")
    private int glowingMushroomsBroken;
    @SerializedName("pumpkin_launcher_count")
    private int pumpkinLauncherCount;

}
