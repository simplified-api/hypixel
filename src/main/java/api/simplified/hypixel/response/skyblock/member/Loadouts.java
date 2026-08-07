package api.simplified.hypixel.response.skyblock.member;

import api.simplified.hypixel.common.NbtContent;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Lenient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

@Getter
public class Loadouts {

    @Lenient
    @SerializedName("armor")
    private @NotNull ConcurrentMap<Integer, ArmorSet> armorSets = Concurrent.newMap();
    @Extract("armorSets.equipped_set")
    private @NotNull Optional<Integer> equippedArmorSet = Optional.empty();

    @Lenient
    @SerializedName("equipment")
    private @NotNull ConcurrentMap<Integer, EquipmentSet> equipmentSets = Concurrent.newMap();
    @Extract("equipmentSets.equipped_set")
    private @NotNull Optional<Integer> equippedEquipmentSet = Optional.empty();

    @Lenient
    @SerializedName("loadouts")
    private @NotNull ConcurrentMap<Integer, Loadout> loadouts = Concurrent.newMap();

    public @NotNull Optional<Loadout> getLoadout(int id) {
        return Optional.ofNullable(this.getLoadouts().get(id));
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ArmorSet {

        private int id;
        @SerializedName("HELMET")
        private @NotNull NbtContent helmet = new NbtContent();
        @SerializedName("CHESTPLATE")
        private @NotNull NbtContent chestplate = new NbtContent();
        @SerializedName("LEGGINGS")
        private @NotNull NbtContent leggings = new NbtContent();
        @SerializedName("BOOTS")
        private @NotNull NbtContent boots = new NbtContent();

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class EquipmentSet {

        private int id;
        @SerializedName("EQUIPMENT_SLOT_1")
        private @NotNull NbtContent necklace = new NbtContent();
        @SerializedName("EQUIPMENT_SLOT_2")
        private @NotNull NbtContent cloak = new NbtContent();
        @SerializedName("EQUIPMENT_SLOT_3")
        private @NotNull NbtContent belt = new NbtContent();
        @SerializedName("EQUIPMENT_SLOT_4")
        private @NotNull NbtContent gloves = new NbtContent();

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Loadout {

        private int id;
        private @NotNull String name = "";
        @SerializedName("armor_set_id")
        private @NotNull Optional<Integer> armorSetId = Optional.empty();
        @SerializedName("equipment_set_id")
        private @NotNull Optional<Integer> equipmentSetId = Optional.empty();
        @SerializedName("power_stone")
        private @NotNull Optional<String> powerStone = Optional.empty();
        private @NotNull Optional<UUID> pet = Optional.empty();
        @SerializedName("mining_core_selected_slot")
        private @NotNull Optional<Integer> miningCoreSlot = Optional.empty();
        @SerializedName("foraging_core_selected_slot")
        private @NotNull Optional<Integer> foragingCoreSlot = Optional.empty();

    }

}
