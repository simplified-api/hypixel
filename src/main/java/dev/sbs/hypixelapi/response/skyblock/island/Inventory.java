package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.linked.ConcurrentLinkedMap;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.util.NbtContent;
import lombok.Getter;

@Getter
public class Inventory {

    @SerializedName("inv_armor")
    private NbtContent armor = new NbtContent();
    @SerializedName("equipment_contents")
    private NbtContent equipment = new NbtContent();
    @SerializedName("wardrobe_contents")
    private NbtContent wardrobe = new NbtContent();
    @SerializedName("bag_contents")
    private BagContents bags = new BagContents();
    @SerializedName("inv_contents")
    private NbtContent content = new NbtContent();
    @SerializedName("wardrobe_equipped_slot")
    private int equippedWardrobeSlot;
    @SerializedName("backpack_icons")
    private ConcurrentMap<Integer, NbtContent> backpackIcons = Concurrent.newMap();
    @SerializedName("personal_vault_contents")
    private NbtContent personalVault = new NbtContent();
    @SerializedName("sacks_counts")
    private ConcurrentLinkedMap<String, Integer> sacks = Concurrent.newLinkedMap();
    @SerializedName("backpack_contents")
    private ConcurrentMap<Integer, NbtContent> backpacks = Concurrent.newMap();
    @SerializedName("ender_chest_contents")
    private NbtContent enderChest = new NbtContent();

    @Getter
    public static class BagContents {

        @SerializedName("fishing_bag")
        private NbtContent fishing = new NbtContent();
        private NbtContent quiver = new NbtContent();
        @SerializedName("fishing_bag")
        private NbtContent accessories = new NbtContent();
        @SerializedName("potion_bag")
        private NbtContent potions = new NbtContent();

    }

}
