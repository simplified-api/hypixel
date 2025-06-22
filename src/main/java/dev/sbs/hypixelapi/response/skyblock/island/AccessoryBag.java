package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.util.NbtContent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccessoryBag {

    private Tuning tuning = new Tuning();
    @SerializedName("selected_power")
    private Optional<String> selectedPower = Optional.empty();
    @SerializedName("bag_upgrades_purchased")
    private int bagUpgradesPurchased;
    @SerializedName("unlocked_powers")
    private @NotNull ConcurrentList<String> unlockedPowers = Concurrent.newList();
    @SerializedName("highest_magical_power")
    private int highestMagicalPower;
    private transient NbtContent contents = new NbtContent();
    @Accessors(fluent = true)
    private transient boolean hasConsumedPrism;
    private transient int abiphoneContacts;

    protected void initialize(@NotNull Member member) {
        this.contents = member.getInventory().getBags().getAccessories();
        this.hasConsumedPrism = member.getRift().getAccess().hasConsumedPrism();
        this.abiphoneContacts = member.getCrimsonIsle().getAbiphone().getContacts().size();
    }

    public static class Tuning {

        @SerializedName("highest_unlocked_slot")
        @Getter private int highestUnlockedSlot;
        @SerializedName("slot_0")
        @Getter private @NotNull ConcurrentMap<String, Integer> current = Concurrent.newMap();
        private @NotNull ConcurrentMap<String, Integer> slot_1 = Concurrent.newMap();
        private @NotNull ConcurrentMap<String, Integer> slot_2 = Concurrent.newMap();
        private @NotNull ConcurrentMap<String, Integer> slot_3 = Concurrent.newMap();
        private @NotNull ConcurrentMap<String, Integer> slot_4 = Concurrent.newMap();

        public @NotNull ConcurrentMap<String, Integer> getSlot(int index) {
            return switch (index) {
                case 1 -> this.slot_1;
                case 2 -> this.slot_2;
                case 3 -> this.slot_3;
                case 4 -> this.slot_4;
                default -> this.current;
            };
        }

        public @NotNull ConcurrentList<ConcurrentMap<String, Integer>> getSlots() {
            return Concurrent.newUnmodifiableList(
                this.slot_1,
                this.slot_2,
                this.slot_3,
                this.slot_4
            );
        }

    }

}
