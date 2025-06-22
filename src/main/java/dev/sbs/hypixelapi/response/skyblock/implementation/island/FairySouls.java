package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class FairySouls {

    @SerializedName("total_collected")
    private int totalCollected;
    @SerializedName("fairy_exchanges")
    private int exchanges;
    @SerializedName("unspent_souls")
    private int unspent;

}
