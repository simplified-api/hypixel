package api.simplified.hypixel.response.skyblock;

import api.simplified.hypixel.common.Experience;
import api.simplified.hypixel.response.skyblock.garden.ActiveCommission;
import api.simplified.hypixel.response.skyblock.garden.CommissionData;
import api.simplified.hypixel.response.skyblock.garden.ComposterData;
import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * The Garden attached to one profile - its farming plots, crop upgrades and visitor commissions.
 * <p>
 * The Garden belongs to the whole island rather than to a member, so it arrives from its own endpoint
 * keyed by profile id rather than inside the profiles response. Its experience drives a progression of
 * its own through {@link Experience}, which is what garden levels and plot unlocks are measured
 * against.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/The_Garden">The Garden</a>
 */
@Getter
@NoArgsConstructor
public class SkyBlockGarden implements Experience {

    /**
     * Unique id of the profile the Garden belongs to.
     */
    @SerializedName("uuid")
    private @NotNull UUID islandId;

    /**
     * Ids of the farming plots cleared so far.
     */
    @SerializedName("unlocked_plot_ids")
    private @NotNull ConcurrentList<String> unlockedPlotIds = Concurrent.newList();

    /**
     * Lifetime totals for the visitors served and the commissions completed for them.
     */
    @SerializedName("commission_data")
    private @NotNull CommissionData commissionData = new CommissionData();

    /**
     * State of the composter, from its stored organic matter to its bought upgrades.
     */
    @SerializedName("composter_data")
    private @NotNull ComposterData composterData = new ComposterData();

    /**
     * Commissions the visitors currently queued are asking for, keyed by visitor id.
     */
    @SerializedName("active_commissions")
    private @NotNull ConcurrentMap<String, ActiveCommission> activeCommissions = Concurrent.newMap();

    /**
     * Garden experience earned, the total behind the garden level.
     */
    private double experience;

    /**
     * Units of each crop harvested in the Garden over its lifetime, keyed by crop item id.
     */
    @SerializedName("resources_collected")
    private @NotNull ConcurrentMap<String, Long> collectedResources = Concurrent.newMap();

    /**
     * Barn skin currently in use, absent while the default barn is standing.
     */
    @SerializedName("selected_barn_skin")
    private @NotNull Optional<String> selectedBarnSkin = Optional.empty();

    /**
     * Barn skins bought and available to switch to.
     */
    @SerializedName("unlocked_barn_skins")
    private @NotNull ConcurrentList<String> unlockedBarnSkins = Concurrent.newList();

    /**
     * Level each crop's own upgrade has reached, keyed by crop item id.
     */
    @SerializedName("crop_upgrade_levels")
    private @NotNull ConcurrentMap<String, Integer> cropUpgradeLevels = Concurrent.newMap();

    /**
     * Level each garden-wide upgrade has reached, keyed by upgrade id.
     */
    @SerializedName("garden_upgrades")
    private @NotNull ConcurrentMap<String, Integer> gardenUpgrades = Concurrent.newMap();

    /**
     * When the Garden's crops last advanced a growth stage.
     */
    private SkyBlockDate.RealTime lastGrowthStageTime;

    /**
     * Greenhouse tiles placed, each carrying its own position.
     */
    @SerializedName("greenhouse_slots")
    private @NotNull ConcurrentList<GreenhouseSlot> greenhouseSlots = Concurrent.newList();

    /** {@inheritDoc} */
    @Override
    public @NotNull ConcurrentList<Integer> getExperienceTiers() {
        return EXPERIENCE_TIERS;
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxLevel() {
        return 15;
    }

    /**
     * Running garden experience needed to reach each garden level, lowest first.
     */
    private static final @NotNull ConcurrentList<Integer> EXPERIENCE_TIERS = Concurrent.newList(
        0, 70, 140, 280, 520, 1_120, 2_620, 4_620, 7_120, 10_120, 20_120, 30_120, 40_120, 50_120, 60_120
    );

    /**
     * One tile of the greenhouse, placed on the Garden's grid.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Greenhouse">Greenhouse</a>
     */
    @Getter
    @NoArgsConstructor
    public static class GreenhouseSlot {

        /**
         * Position of the tile along the grid's first axis.
         */
        private int x;

        /**
         * Position of the tile along the grid's second axis.
         */
        private int z;

    }

}
