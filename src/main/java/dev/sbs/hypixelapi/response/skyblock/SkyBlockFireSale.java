package dev.sbs.hypixelapi.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.date.SkyBlockDate;
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
