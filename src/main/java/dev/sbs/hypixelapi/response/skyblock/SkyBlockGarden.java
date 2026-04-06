package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.garden.ActiveCommission;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.garden.CommissionData;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.garden.ComposterData;
import dev.sbs.minecraftapi.skyblock.common.Experience;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class SkyBlockGarden implements Experience {

    @SerializedName("uuid")
    private @NotNull UUID islandId;
    @SerializedName("unlocked_plot_ids")
    private @NotNull ConcurrentList<String> unlockedPlotIds = Concurrent.newList();
    @SerializedName("commission_data")
    private @NotNull CommissionData commissionData = new CommissionData();
    @SerializedName("composter_data")
    private @NotNull ComposterData composterData = new ComposterData();
    @SerializedName("active_commissions")
    private @NotNull ConcurrentMap<String, ActiveCommission> activeCommissions = Concurrent.newMap();
    private double experience;
    @SerializedName("resources_collected")
    private @NotNull ConcurrentMap<String, Long> collectedResources = Concurrent.newMap();
    @SerializedName("selected_barn_skin")
    private @NotNull Optional<String> selectedBarnSkin = Optional.empty();
    @SerializedName("unlocked_barn_skins")
    private @NotNull ConcurrentList<String> unlockedBarnSkins = Concurrent.newList();
    @SerializedName("crop_upgrade_levels")
    private @NotNull ConcurrentMap<String, Integer> cropUpgradeLevels = Concurrent.newMap();
    @SerializedName("garden_upgrades")
    private @NotNull ConcurrentMap<String, Integer> gardenUpgrades = Concurrent.newMap();
    private SkyBlockDate.RealTime lastGrowthStageTime;
    @SerializedName("greenhouse_slots")
    private @NotNull ConcurrentList<GreenhouseSlot> greenhouseSlots = Concurrent.newList();

    @Override
    public @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return EXPERIENCE_TIERS;
    }

    @Override
    public int getMaxLevel() {
        return 15;
    }

    private static final @NotNull ConcurrentList<Integer> EXPERIENCE_TIERS = Concurrent.newList(
        0, 70, 140, 280, 520, 1_120, 2_620, 4_620, 7_120, 10_120, 20_120, 30_120, 40_120, 50_120, 60_120
    );

    @Getter
    @NoArgsConstructor
    public static class GreenhouseSlot {

        private int x;
        private int z;

    }

}
