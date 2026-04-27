package api.simplified.hypixel.response.skyblock.member;

import api.simplified.hypixel.common.NbtContent;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class SharedInventory {

    @SerializedName("carnival_mask_inventory_contents")
    private NbtContent carnivalMasks = new NbtContent();
    @SerializedName("candy_inventory_contents")
    private NbtContent candy = new NbtContent();

}
