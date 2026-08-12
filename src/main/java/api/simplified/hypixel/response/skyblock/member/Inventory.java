package api.simplified.hypixel.response.skyblock.member;

import api.simplified.hypixel.common.NbtContent;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentLinkedMap;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Everything a member is carrying on one profile - worn armor and equipment, the wardrobe, the ender
 * chest, the storage backpacks, the personal vault, the sacks and the item bags.
 *
 * <p>
 * Every item store here is an {@link NbtContent}, which is base64 of gzipped NBT rather than
 * readable JSON, so an item can only be read by parsing the blob. An absent store binds as an empty
 * {@link NbtContent} rather than as null.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Storage">Storage</a>
 */
@Getter
public class Inventory {

    /**
     * The four worn armor pieces.
     */
    @SerializedName("inv_armor")
    private @NotNull NbtContent armor = new NbtContent();

    /**
     * The four worn equipment pieces.
     */
    @SerializedName("equipment_contents")
    private @NotNull NbtContent equipment = new NbtContent();

    /**
     * Every saved wardrobe page.
     */
    @SerializedName("wardrobe_contents")
    private @NotNull NbtContent wardrobe = new NbtContent();

    /**
     * The item bags and the sack of sacks.
     */
    @SerializedName("bag_contents")
    private @NotNull Bags bags = new Bags();

    /**
     * The main inventory the member carries.
     */
    @SerializedName("inv_contents")
    private @NotNull NbtContent content = new NbtContent();

    /**
     * Which wardrobe slot the member is wearing.
     */
    @SerializedName("wardrobe_equipped_slot")
    private int equippedWardrobeSlot;

    /**
     * The icon item shown for each storage backpack, keyed by backpack index.
     */
    @SerializedName("backpack_icons")
    private @NotNull ConcurrentMap<Integer, NbtContent> backpackIcons = Concurrent.newMap();

    /**
     * The personal vault.
     */
    @SerializedName("personal_vault_contents")
    private @NotNull NbtContent personalVault = new NbtContent();

    /**
     * Quantity stored in the sacks per item id, kept in the order the wire sent them. Item ids carry
     * colons, so {@code RAW_FISH:1} is a different entry from {@code RAW_FISH}, and a count of
     * {@code 0} is a row that exists holding nothing rather than an absent row.
     */
    @SerializedName("sacks_counts")
    private @NotNull ConcurrentLinkedMap<String, Integer> sacks = Concurrent.newLinkedMap();

    /**
     * The contents of each storage backpack, keyed by backpack index.
     */
    @SerializedName("backpack_contents")
    private @NotNull ConcurrentMap<Integer, NbtContent> backpacks = Concurrent.newMap();

    /**
     * The ender chest.
     */
    @SerializedName("ender_chest_contents")
    private @NotNull NbtContent enderChest = new NbtContent();


    /**
     * The item bags a member carries, plus the sack of sacks.
     */
    @Getter
    public static class Bags {

        /**
         * The accessory bag contents, the value a member hands to
         * {@link AccessoryBag#initialize(NbtContent, boolean, int)}.
         */
        @SerializedName("talisman_bag")
        private @NotNull NbtContent accessories = new NbtContent();

        /**
         * Bait and fish held in the fishing bag.
         */
        @SerializedName("fishing_bag")
        private @NotNull NbtContent fishing = new NbtContent();

        /**
         * Potions held in the potion bag.
         */
        @SerializedName("potion_bag")
        private @NotNull NbtContent potions = new NbtContent();

        /**
         * Arrows and arrow poisons held in the quiver.
         */
        @SerializedName("quiver")
        private @NotNull NbtContent quiver = new NbtContent();

        /**
         * The sack of sacks.
         */
        @SerializedName("sacks_bag")
        private @NotNull NbtContent sacks = new NbtContent();

    }

}
