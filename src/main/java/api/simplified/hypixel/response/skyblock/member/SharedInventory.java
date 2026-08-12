package api.simplified.hypixel.response.skyblock.member;

import api.simplified.hypixel.common.NbtContent;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

/**
 * Item stores that persist across all of a member's profiles rather than living on one of them.
 *
 * <p>
 * Both are an {@link NbtContent} - base64 of gzipped NBT rather than readable JSON - and both start
 * out as an empty blob, so an absent wire key never reaches a caller as null.
 */
@Getter
public class SharedInventory {

    /**
     * The carnival mask bag.
     */
    @SerializedName("carnival_mask_inventory_contents")
    private NbtContent carnivalMasks = new NbtContent();

    /**
     * The Spooky Festival candy bag.
     */
    @SerializedName("candy_inventory_contents")
    private NbtContent candy = new NbtContent();

}
