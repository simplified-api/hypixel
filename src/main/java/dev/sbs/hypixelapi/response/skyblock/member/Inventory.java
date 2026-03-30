package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.linked.ConcurrentLinkedMap;
import dev.sbs.minecraftapi.skyblock.common.NbtContent;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class Inventory {

    @SerializedName("inv_armor")
    private @NotNull NbtContent armor = new NbtContent();
    @SerializedName("equipment_contents")
    private @NotNull NbtContent equipment = new NbtContent();
    @SerializedName("wardrobe_contents")
    private @NotNull NbtContent wardrobe = new NbtContent();
    @SerializedName("bag_contents")
    private @NotNull Bags bags = new Bags();
    @SerializedName("inv_contents")
    private @NotNull NbtContent content = new NbtContent();
    @SerializedName("wardrobe_equipped_slot")
    private int equippedWardrobeSlot;
    @SerializedName("backpack_icons")
    private @NotNull ConcurrentMap<Integer, NbtContent> backpackIcons = Concurrent.newMap();
    @SerializedName("personal_vault_contents")
    private @NotNull NbtContent personalVault = new NbtContent();
    @SerializedName("sacks_counts")
    private @NotNull ConcurrentLinkedMap<String, Integer> sacks = Concurrent.newLinkedMap();
    @SerializedName("backpack_contents")
    private @NotNull ConcurrentMap<Integer, NbtContent> backpacks = Concurrent.newMap();
    @SerializedName("ender_chest_contents")
    private @NotNull NbtContent enderChest = new NbtContent();


    @Getter
    public static class Bags {

        @SerializedName("talisman_bag")
        private @NotNull NbtContent accessories = new NbtContent();
        @SerializedName("fishing_bag")
        private @NotNull NbtContent fishing = new NbtContent();
        @SerializedName("potion_bag")
        private @NotNull NbtContent potions = new NbtContent();
        @SerializedName("quiver")
        private @NotNull NbtContent quiver = new NbtContent();
        @SerializedName("sacks_bag")
        private @NotNull NbtContent sacks = new NbtContent();

    }

}
