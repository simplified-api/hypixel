package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.accessories;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.ConcurrentSet;
import dev.sbs.api.collection.search.SearchFunction;
import dev.sbs.minecraftapi.data.model.accessory_data.accessories.AccessoryModel;
import dev.sbs.minecraftapi.data.model.accessory_data.accessory_families.AccessoryFamilyModel;
import dev.sbs.minecraftapi.data.model.accessory_data.accessory_powers.AccessoryPowerModel;
import dev.sbs.minecraftapi.data.model.items.ItemModel;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.profile_stats.data.AccessoryData;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

@Getter
public class EnhancedAccessoryBag extends AccessoryBag {

    private final @NotNull ConcurrentList<AccessoryData> accessories;
    private final @NotNull ConcurrentList<AccessoryData> filteredAccessories;
    private final @NotNull Optional<AccessoryPowerModel> selectedPowerType;
    private final int magicalPower;
    private final int tuningPoints;
    private final double magicalPowerMultiplier;

    EnhancedAccessoryBag(@NotNull AccessoryBag accessoryBag) {
        // Re-initialize Fields
        super(
            accessoryBag.getTuning(),
            accessoryBag.getSelectedPower(),
            accessoryBag.getBagUpgradesPurchased(),
            accessoryBag.getUnlockedPowers(),
            accessoryBag.getHighestMagicalPower(),
            accessoryBag.getContents(),
            accessoryBag.hasConsumedPrism(),
            accessoryBag.getAbiphoneContacts()
        );
        ConcurrentMap<CompoundTag, AccessoryModel> tagAccessoryModels = Concurrent.newMap();

        // Load From Accessory Bag
        if (this.getContents() != null) {
            this.getContents()
                .getNbtData()
                .<CompoundTag>getListTag("i")
                .stream()
                .filter(CompoundTag::notEmpty)
                .forEach(compoundTag -> SimplifiedApi.getRepositoryOf(AccessoryModel.class)
                    .findFirst(
                        SearchFunction.combine(AccessoryModel::getItem, ItemModel::getItemId),
                        compoundTag.getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY).getValue()
                    )
                    .ifPresent(accessoryModel -> tagAccessoryModels.put(compoundTag, accessoryModel))
                );
        }

        // Create Accessory Data
        this.accessories = tagAccessoryModels.stream()
            .map(entry -> new AccessoryData(entry.getValue(), entry.getKey()))
            .collect(Concurrent.toList());

        // Store Families
        ConcurrentMap<AccessoryFamilyModel, ConcurrentSet<AccessoryModel>> familyAccessoryDataMap = Concurrent.newMap();
        this.accessories.stream()
            .filter(accessoryData -> Objects.nonNull(accessoryData.getAccessory().getFamily()))
            .forEach(accessoryData -> {
                // New Accessory Family
                if (!familyAccessoryDataMap.containsKey(accessoryData.getAccessory().getFamily()))
                    familyAccessoryDataMap.put(accessoryData.getAccessory().getFamily(), Concurrent.newSet());

                // Store Accessory
                familyAccessoryDataMap.get(accessoryData.getAccessory().getFamily()).add(accessoryData.getAccessory());
            });

        // Store Non-Stackable Families
        ConcurrentSet<AccessoryModel> processedAccessories = Concurrent.newSet();
        this.filteredAccessories = accessories.stream()
            .filter(accessoryData -> {
                AccessoryFamilyModel accessoryFamilyModel = accessoryData.getAccessory().getFamily();

                // Handle Families
                if (Objects.nonNull(accessoryFamilyModel)) {
                    if (accessoryFamilyModel.isStatsStackable())
                        return true;
                    else if (accessoryFamilyModel.isReforgesStackable())
                        return true;
                    else {
                        ConcurrentList<AccessoryModel> familyData = Concurrent.newList(familyAccessoryDataMap.get(accessoryFamilyModel));

                        if (accessoryData.getAccessory().getFamilyRank() != null) {
                            familyData = familyData.sorted(AccessoryModel::getFamilyRank)
                                .inverse(); // Sort By Highest

                            // Ignore Lowest Accessories
                            AccessoryModel topAccessory = familyData.remove(0);
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
                }

                return processedAccessories.add(accessoryData.getAccessory());
            })
            .collect(Concurrent.toList());

        int currentMagicalPower = this.filteredAccessories.stream()
            .mapToInt(this::handleMagicalPower)
            .sum();

        // Rift Prism
        if (this.hasConsumedPrism())
            currentMagicalPower += 11;

        this.magicalPower = currentMagicalPower;
        this.magicalPowerMultiplier = 29.97 * Math.pow(Math.log(0.0019 * this.magicalPower + 1), 1.2);
        this.tuningPoints = this.magicalPower / 10;
        this.selectedPowerType = this.getSelectedPower().flatMap(selectedPower -> SimplifiedApi.getRepositoryOf(AccessoryPowerModel.class).findFirst(AccessoryPowerModel::getKey, selectedPower));
    }

    private int handleMagicalPower(@NotNull AccessoryData accessoryData) {
        int magicalPower = accessoryData.getRarity().getMagicPowerMultiplier();

        if (accessoryData.getItem().getItemId().equals("HEGEMONY_ARTIFACT"))
            magicalPower *= 2;

        if (accessoryData.getItem().getItemId().equals("ABICASE"))
            magicalPower += this.getAbiphoneContacts() / 2;

        return magicalPower;
    }

}
