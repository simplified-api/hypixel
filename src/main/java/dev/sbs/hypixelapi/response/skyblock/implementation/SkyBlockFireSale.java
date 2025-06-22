package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import lombok.Getter;

@Getter
public class SkyBlockFireSale {

    @SerializedName("item_id")
    private String itemId;
    private SkyBlockDate.RealTime start;
    private SkyBlockDate.RealTime end;
    private int amount;
    private int price;

}
