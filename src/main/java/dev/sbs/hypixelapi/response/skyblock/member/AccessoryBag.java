package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockMember;
import dev.sbs.minecraftapi.persistence.model.Power;
import dev.sbs.minecraftapi.persistence.model.Stat;
import dev.sbs.minecraftapi.skyblock.common.NbtContent;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.gson.PostInit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class AccessoryBag {

    @SerializedName("bag_upgrades_purchased")
    private int bagUpgradesPurchased;
    private transient NbtContent contents = new NbtContent();
    //private transient @NotNull ConcurrentList<AccessoryData> detectedAccessories = Concurrent.newUnmodifiableList();
    //private transient @NotNull ConcurrentList<AccessoryData> accessories = Concurrent.newUnmodifiableList();

    // Power
    @SerializedName("selected_power")
    private @NotNull Optional<String> selectedPowerId = Optional.empty();
    @SerializedName("unlocked_powers")
    private @NotNull ConcurrentList<String> unlockedPowerIds = Concurrent.newUnmodifiableList();
    private transient @NotNull ConcurrentMap<String, Double> selectedPowerStats = Concurrent.newUnmodifiableMap();

    // Magical Power
    @SerializedName("highest_magical_power")
    private int highestMagicalPower;
    private transient int magicalPower;
    private transient double logComponent;

    // Tuning
    private @NotNull Tuning tuning = new Tuning();
    private transient int tuningPoints;

    public void initialize(@NotNull SkyBlockMember member) {
        // Read Accessory Bag
        /*this.detectedAccessories = this.getContents()
            .getNbtData()
            .<CompoundTag>getListTag("i")
            .stream()
            .filter(CompoundTag::notEmpty)
            .flatMap(compoundTag -> SimplifiedApi.getRepository(Accessory.class)
                .findFirst(
                    Accessory::getId,
                    compoundTag.getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY).getValue()
                )
                .map(accessory -> Pair.of(accessory, compoundTag))
                .stream()
            )
            .map(entry -> new AccessoryData(entry.getKey(), entry.getValue()))
            .collect(Concurrent.toList());

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
                        familyData = familyData.sorted(accessory -> accessory.getFamily()
                                .map(Accessory.Family::getRank)
                                .orElse(0)
                            )
                            .reversed();

                        // Ignore Lowest Accessories
                        Accessory topAccessory = familyData.remove(0);
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

        // Calculate Magical Power
        int calculatedMagicalPower = this.getAccessories()
            .stream()
            .mapToInt(accessoryData -> this.handleMagicalPower(accessoryData, member))
            .sum();

        // Rift Prism
        if (member.getRift().getAccess().hasConsumedPrism())
            calculatedMagicalPower += 11;*/

        this.contents = member.getInventory().getBags().getAccessories();
        this.magicalPower = 0; // TODO: calculatedMagicalPower
        this.tuningPoints = this.magicalPower / 10;
        this.logComponent = Math.pow(Math.log(1 + (0.0019 * this.magicalPower)), 1.2);
        //this.magicalPowerMultiplier = 29.97 * Math.pow(Math.log(1 + (0.0019 * this.magicalPower)), 1.2);

        // Power Stats
        ConcurrentMap<String, Double> stats = this.getSelectedPower()
            .stream()
            .flatMap(power -> power.getBaseValues().stream())
            .map(entry -> Pair.of(
                entry.getKey(),
                MinecraftApi.getRepository(Stat.class)
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

    public @NotNull Optional<Power> getSelectedPower() {
        return this.getSelectedPowerId().flatMap(powerId -> MinecraftApi.getRepository(Power.class)
            .findFirst(Power::getId, powerId)
        );
    }

    public @NotNull ConcurrentList<Power> getUnlockedPowers() {
        return this.getUnlockedPowerIds()
            .stream()
            .map(powerId -> MinecraftApi.getRepository(Power.class)
                .findFirst(Power::getId, powerId)
            )
            .flatMap(Optional::stream)
            .collect(Concurrent.toUnmodifiableList());
    }

    /*private int handleMagicalPower(@NotNull AccessoryData accessoryData, @NotNull SkyBlockMember member) {
        int magicalPower = accessoryData.getRarity().getMagicPower();

        // TODO: Dynamic
        if (accessoryData.getAccessory().getId().equals("HEGEMONY_ARTIFACT"))
            magicalPower *= 2;

        if (accessoryData.getAccessory().getId().equals("ABICASE"))
            magicalPower += member.getCrimsonIsle().getAbiphone().getContacts().size() / 2;

        return magicalPower;
    }*/

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Tuning implements PostInit {

        @SerializedName("highest_unlocked_slot")
        private int highestUnlockedSlot;
        @SerializedName("refund_1")
        @Accessors(fluent = true)
        private boolean hasClaimedRefund;

        // Slots
        @SerializedName("slot_0")
        private @NotNull ConcurrentMap<String, Integer> selected = Concurrent.newMap();
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, Integer> slot_1 = Concurrent.newMap();
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, Integer> slot_2 = Concurrent.newMap();
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, Integer> slot_3 = Concurrent.newMap();
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, Integer> slot_4 = Concurrent.newMap();

        // PostInit
        private @NotNull ConcurrentList<ConcurrentMap<String, Integer>> slots = Concurrent.newList();

        @Override
        public void postInit() {
            this.slots = Concurrent.newUnmodifiableList(
                this.slot_1,
                this.slot_2,
                this.slot_3,
                this.slot_4
            );
        }

    }

}
