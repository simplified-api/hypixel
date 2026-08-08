package api.simplified.hypixel.response.skyblock.member;

import api.simplified.hypixel.common.NbtContent;
import api.simplified.hypixel.profile_stats.data.AccessoryData;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.Accessory;
import api.simplified.skyblock.model.Power;
import api.simplified.skyblock.model.Stat;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.ConcurrentSet;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.gson.annotation.Capture;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.StringTag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Function;

@Getter
public class AccessoryBag {

    @SerializedName("bag_upgrades_purchased")
    private int bagUpgradesPurchased;

    // Member-scoped values, supplied by initialize because they live outside this node
    private transient @NotNull NbtContent contents = new NbtContent();
    @Getter(AccessLevel.NONE)
    private transient boolean consumedPrism;
    @Getter(AccessLevel.NONE)
    private transient int abiphoneContacts;

    @Getter(AccessLevel.NONE)
    private transient ConcurrentList<AccessoryData> detectedAccessories;
    @Getter(AccessLevel.NONE)
    private transient ConcurrentList<AccessoryData> accessories;

    // Power
    @SerializedName("selected_power")
    private @NotNull Optional<String> selectedPowerId = Optional.empty();
    @SerializedName("unlocked_powers")
    private @NotNull ConcurrentList<String> unlockedPowerIds = Concurrent.newUnmodifiableList();
    @Getter(AccessLevel.NONE)
    private transient ConcurrentMap<String, Double> selectedPowerStats;

    // Magical Power
    @SerializedName("highest_magical_power")
    private int highestMagicalPower;
    @Getter(AccessLevel.NONE)
    private transient Integer magicalPower;

    // Tuning
    private @NotNull Tuning tuning = new Tuning();

    /**
     * Supplies the three member-scoped values this bag cannot reach from its own node.
     *
     * @param contents the talisman bag item data, stored under the member's inventory
     * @param consumedPrism whether the rift prism has been consumed
     * @param abiphoneContacts the abiphone contact count, halved by an equipped abicase
     * @return this bag
     */
    public @NotNull AccessoryBag initialize(@NotNull NbtContent contents, boolean consumedPrism, int abiphoneContacts) {
        this.contents = contents;
        this.consumedPrism = consumedPrism;
        this.abiphoneContacts = abiphoneContacts;
        return this;
    }

    /**
     * Accessories parsed out of the talisman bag and resolved against the accessory repository,
     * empty for a bag decoded on its own
     */
    public @NotNull ConcurrentList<AccessoryData> getDetectedAccessories() {
        if (this.detectedAccessories == null) {
            this.detectedAccessories = this.contents.getRawData().isEmpty()
                ? Concurrent.newUnmodifiableList()
                : this.contents
                    .getNbtData()
                    .<CompoundTag>getListTag("i")
                    .stream()
                    .filter(CompoundTag::notEmpty)
                    .flatMap(compoundTag -> SkyBlockData.getRepository(Accessory.class)
                        .findFirst(
                            Accessory::getId,
                            compoundTag.getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY).getValue()
                        )
                        .map(accessory -> Pair.of(accessory, compoundTag))
                        .stream()
                    )
                    .map(entry -> new AccessoryData(entry.getKey(), entry.getValue()))
                    .collect(Concurrent.toList());
        }

        return this.detectedAccessories;
    }

    /**
     * The detected accessories that count toward magical power, with the lower-ranked members of each
     * family dropped
     */
    public @NotNull ConcurrentList<AccessoryData> getAccessories() {
        if (this.accessories != null)
            return this.accessories;

        // Store Families
        ConcurrentMap<String, ConcurrentSet<Accessory>> familyAccessoryDataMap = Concurrent.newMap();
        this.getDetectedAccessories()
            .stream()
            .filter(accessoryData -> accessoryData.getAccessory().getFamily().isPresent())
            .forEach(accessoryData -> {
                // New Accessory Family
                String familyId = accessoryData.getAccessory().getFamily().get().getId();
                if (!familyAccessoryDataMap.containsKey(familyId))
                    familyAccessoryDataMap.put(familyId, Concurrent.newSet());

                // Store Accessory
                familyAccessoryDataMap.get(familyId).add(accessoryData.getAccessory());
            });

        // Store Non-Stackable Families
        ConcurrentSet<Accessory> processedAccessories = Concurrent.newSet();
        this.accessories = this.getDetectedAccessories()
            .stream()
            .filter(accessoryData -> {
                if (accessoryData.getAccessory().getFamily().isPresent()) {
                    // Handle Families
                    ConcurrentList<Accessory> familyData = Concurrent.newList(familyAccessoryDataMap.get(
                        accessoryData.getAccessory().getFamily().get().getId()
                    ));

                    if (accessoryData.getAccessory().getFamily().get().getRank() >= 0) {
                        // Sort By Highest
                        Function<Accessory, Integer> byFamilyRank = accessory -> accessory.getFamily()
                            .map(Accessory.Family::getRank)
                            .orElse(0);

                        familyData = familyData.sorted(byFamilyRank).reversed();

                        // Ignore Lowest Accessories
                        Accessory topAccessory = familyData.removeFirst();
                        processedAccessories.addAll(familyData);

                        // Top Accessory Only
                        if (!accessoryData.getAccessory().equals(topAccessory))
                            return false;
                    } else {
                        if (processedAccessories.contains(accessoryData.getAccessory()))
                            return false;

                        // Ignore All Accessories
                        processedAccessories.addAll(familyData);
                        return true;
                    }
                }

                return processedAccessories.add(accessoryData.getAccessory());
            })
            .collect(Concurrent.toList());

        return this.accessories;
    }

    /**
     * Magical power granted by the counting accessories, plus the rift prism bonus
     */
    public int getMagicalPower() {
        if (this.magicalPower == null) {
            int calculated = this.getAccessories()
                .stream()
                .mapToInt(this::handleMagicalPower)
                .sum();

            // Rift Prism
            if (this.consumedPrism)
                calculated += 11;

            this.magicalPower = calculated;
        }

        return this.magicalPower;
    }

    public int getTuningPoints() {
        return this.getMagicalPower() / 10;
    }

    public double getLogComponent() {
        return Math.pow(Math.log(1 + (0.0019 * this.getMagicalPower())), 1.2);
    }

    /**
     * Stats granted by the selected power, scaled by its coefficient and this bag's magical power
     */
    public @NotNull ConcurrentMap<String, Double> getSelectedPowerStats() {
        if (this.selectedPowerStats == null) {
            ConcurrentMap<String, Double> stats = this.getSelectedPower()
                .stream()
                .flatMap(power -> power.getBaseValues().stream())
                .map(entry -> Pair.of(
                    entry.getKey(),
                    SkyBlockData.getRepository(Stat.class)
                        .findFirstOrNull(Stat::getId, entry.getKey())
                        .getPowerCoefficient() * this.getLogComponent() * entry.getValue()
                ))
                .collect(Concurrent.toUnmodifiableMap());

            this.getSelectedPower().ifPresent(power -> power.getBonuses()
                .forEach((statId, value) -> stats.merge(
                    statId,
                    value,
                    Double::sum
                ))
            );

            this.selectedPowerStats = stats;
        }

        return this.selectedPowerStats;
    }

    public @NotNull Optional<Power> getSelectedPower() {
        return this.getSelectedPowerId().flatMap(powerId -> SkyBlockData.getRepository(Power.class)
            .findFirst(Power::getId, powerId)
        );
    }

    public @NotNull ConcurrentList<Power> getUnlockedPowers() {
        return this.getUnlockedPowerIds()
            .stream()
            .map(powerId -> SkyBlockData.getRepository(Power.class)
                .findFirst(Power::getId, powerId)
            )
            .flatMap(Optional::stream)
            .collect(Concurrent.toUnmodifiableList());
    }

    private int handleMagicalPower(@NotNull AccessoryData accessoryData) {
        int magicalPower = accessoryData.getRarity().getMagicPower();

        // TODO: Dynamic
        if (accessoryData.getAccessory().getId().equals("HEGEMONY_ARTIFACT"))
            magicalPower *= 2;

        if (accessoryData.getAccessory().getId().equals("ABICASE"))
            magicalPower += this.abiphoneContacts / 2;

        return magicalPower;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Tuning {

        @SerializedName("highest_unlocked_slot")
        private int highestUnlockedSlot;
        @SerializedName("refund_1")
        @Accessors(fluent = true)
        private boolean hasClaimedRefund;
        @SerializedName("refund_2")
        @Accessors(fluent = true)
        private boolean hasClaimedSecondRefund;

        @Capture(filter = "^slot_")
        private @NotNull ConcurrentMap<Integer, Slot> slots = Concurrent.newMap();

        public @NotNull Optional<Slot> getSlot(int slot) {
            return Optional.ofNullable(this.getSlots().get(slot));
        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Slot {

            @SerializedName("purchase_ts")
            private @NotNull Optional<SkyBlockDate.RealTime> purchased = Optional.empty();
            @Capture
            private @NotNull ConcurrentMap<String, Integer> stats = Concurrent.newMap();

        }

    }

}
