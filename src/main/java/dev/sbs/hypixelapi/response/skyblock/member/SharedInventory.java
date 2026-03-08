package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.common.NbtContent;
import lombok.Getter;

@Getter
public class SharedInventory {

    @SerializedName("carnival_mask_inventory_contents")
    private NbtContent carnivalMasks = new NbtContent();
    @SerializedName("candy_inventory_contents")
    private NbtContent candy = new NbtContent();

}
