package api.simplified.hypixel.response.skyblock.member;

import api.simplified.hypixel.common.NbtContent;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Lenient;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * The saved armour sets, equipment sets and loadouts a member can switch between.
 * <p>
 * A loadout is a reference rather than a copy - it names an armour set and an equipment set by id
 * instead of carrying the items, so the same set can back several loadouts. Which set is currently
 * worn is sent alongside the sets themselves rather than inside them, which is why it is lifted out
 * of each map instead of binding from a root key.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Loadouts">Loadouts</a>
 */
@Getter
public class Loadouts {

    /**
     * Each saved armour set, keyed by its id.
     */
    @Lenient
    @SerializedName("armor")
    private @NotNull ConcurrentMap<Integer, ArmorSet> armorSets = Concurrent.newMap();

    /**
     * Id of the armour set currently worn, lifted out of the armour node, empty when none is.
     */
    @Extract("armorSets.equipped_set")
    private @NotNull Optional<Integer> equippedArmorSet = Optional.empty();

    /**
     * Each saved equipment set, keyed by its id.
     */
    @Lenient
    @SerializedName("equipment")
    private @NotNull ConcurrentMap<Integer, EquipmentSet> equipmentSets = Concurrent.newMap();

    /**
     * Id of the equipment set currently worn, lifted out of the equipment node, empty when none is.
     */
    @Extract("equipmentSets.equipped_set")
    private @NotNull Optional<Integer> equippedEquipmentSet = Optional.empty();

    /**
     * Each saved loadout, keyed by its id.
     */
    @Lenient
    @SerializedName("loadouts")
    private @NotNull ConcurrentMap<Integer, Loadout> loadouts = Concurrent.newMap();

    /**
     * Reads one saved loadout.
     *
     * @param id the loadout id to read
     * @return that loadout, empty when none carries the id
     */
    public @NotNull Optional<Loadout> getLoadout(int id) {
        return Optional.ofNullable(this.getLoadouts().get(id));
    }

    /**
     * One saved set of the four armour pieces.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ArmorSet {

        /**
         * Id this set is keyed by.
         */
        private int id;

        /**
         * The helmet saved in this set, empty when the slot was left unfilled.
         */
        @SerializedName("HELMET")
        private @NotNull NbtContent helmet = new NbtContent();

        /**
         * The chestplate saved in this set, empty when the slot was left unfilled.
         */
        @SerializedName("CHESTPLATE")
        private @NotNull NbtContent chestplate = new NbtContent();

        /**
         * The leggings saved in this set, empty when the slot was left unfilled.
         */
        @SerializedName("LEGGINGS")
        private @NotNull NbtContent leggings = new NbtContent();

        /**
         * The boots saved in this set, empty when the slot was left unfilled.
         */
        @SerializedName("BOOTS")
        private @NotNull NbtContent boots = new NbtContent();

    }

    /**
     * One saved set of the four equipment pieces, which are worn alongside armour.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class EquipmentSet {

        /**
         * Id this set is keyed by.
         */
        private int id;

        /**
         * The necklace saved in this set, the first equipment slot on the wire.
         */
        @SerializedName("EQUIPMENT_SLOT_1")
        private @NotNull NbtContent necklace = new NbtContent();

        /**
         * The cloak saved in this set, the second equipment slot on the wire.
         */
        @SerializedName("EQUIPMENT_SLOT_2")
        private @NotNull NbtContent cloak = new NbtContent();

        /**
         * The belt saved in this set, the third equipment slot on the wire.
         */
        @SerializedName("EQUIPMENT_SLOT_3")
        private @NotNull NbtContent belt = new NbtContent();

        /**
         * The gloves saved in this set, the fourth equipment slot on the wire.
         */
        @SerializedName("EQUIPMENT_SLOT_4")
        private @NotNull NbtContent gloves = new NbtContent();

    }

    /**
     * One saved loadout, naming the sets and selections it applies rather than carrying them.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Loadout {

        /**
         * Id this loadout is keyed by.
         */
        private int id;

        /**
         * Name the member gave the loadout.
         */
        private @NotNull String name = "";

        /**
         * Id of the armour set this loadout equips, empty when it changes no armour.
         */
        @SerializedName("armor_set_id")
        private @NotNull Optional<Integer> armorSetId = Optional.empty();

        /**
         * Id of the equipment set this loadout equips, empty when it changes no equipment.
         */
        @SerializedName("equipment_set_id")
        private @NotNull Optional<Integer> equipmentSetId = Optional.empty();

        /**
         * Id of the accessory bag power this loadout selects, empty when it changes no power.
         */
        @SerializedName("power_stone")
        private @NotNull Optional<String> powerStone = Optional.empty();

        /**
         * Identifier of the pet this loadout summons, empty when it changes no pet.
         */
        private @NotNull Optional<UUID> pet = Optional.empty();

        /**
         * Heart of the Mountain preset this loadout selects, empty when it changes none.
         */
        @SerializedName("mining_core_selected_slot")
        private @NotNull Optional<Integer> miningCoreSlot = Optional.empty();

        /**
         * Heart of the Forest preset this loadout selects, empty when it changes none.
         */
        @SerializedName("foraging_core_selected_slot")
        private @NotNull Optional<Integer> foragingCoreSlot = Optional.empty();

    }

}
