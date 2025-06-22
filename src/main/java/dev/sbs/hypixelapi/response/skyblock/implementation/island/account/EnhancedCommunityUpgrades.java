package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.account;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.data.model.shop_data.shop_profile_upgrades.ShopProfileUpgradeModel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

@Getter
public class EnhancedCommunityUpgrades extends CommunityUpgrades {

    public EnhancedCommunityUpgrades(@NotNull CommunityUpgrades communityUpgrades) {
        super(
            communityUpgrades.getCurrentlyUpgrading(),
            communityUpgrades.getUpgraded()
        );
    }

    public int getHighestTier(@NotNull ShopProfileUpgradeModel shopProfileUpgradeModel) {
        return this.getUpgraded()
            .stream()
            .filter(upgraded -> upgraded.getUpgrade().name().equalsIgnoreCase(shopProfileUpgradeModel.getKey()))
            .sorted((o1, o2) -> Comparator.comparing(Upgraded::getTier).compare(o2, o1))
            .map(Upgraded::getTier)
            .findFirst()
            .orElse(0);
    }

    public @NotNull ConcurrentList<Upgraded> getUpgrades(@NotNull ShopProfileUpgradeModel shopProfileUpgradeModel) {
        return this.getUpgraded()
            .stream()
            .filter(upgraded -> upgraded.getUpgrade().name().equalsIgnoreCase(shopProfileUpgradeModel.getKey()))
            .sorted((o1, o2) -> Comparator.comparing(Upgraded::getTier).compare(o1, o2))
            .collect(Concurrent.toList());
    }

}
