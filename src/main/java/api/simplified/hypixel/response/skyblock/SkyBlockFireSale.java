package api.simplified.hypixel.response.skyblock;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
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
