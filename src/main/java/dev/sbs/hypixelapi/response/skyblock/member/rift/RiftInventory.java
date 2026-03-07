package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.common.NbtContent;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class RiftInventory {

    @SerializedName("inv_contents")
    private @NotNull NbtContent inventory = new NbtContent();
    @SerializedName("inv_armor")
    private @NotNull NbtContent armor = new NbtContent();
    @SerializedName("ender_chest_contents")
    private @NotNull NbtContent enderChest = new NbtContent();
    @SerializedName("equipment_contents")
    private @NotNull NbtContent equipment = new NbtContent();

}
