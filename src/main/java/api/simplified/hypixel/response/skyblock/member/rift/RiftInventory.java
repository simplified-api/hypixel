package api.simplified.hypixel.response.skyblock.member.rift;

import api.simplified.hypixel.common.NbtContent;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

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
    @SerializedName("ender_chest_page_icons")
    private @NotNull ConcurrentList<Optional<NbtContent>> enderChestPageIcons = Concurrent.newList();

}
