package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * A member's own state on the Garden, the private farming island.
 *
 * <p>
 * The Garden's level, its visitors and its plots are not on this node.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/The_Garden">The Garden</a>
 */
@Getter
public class GardenCore {

    /**
     * Copper held, the Garden's own currency.
     */
    private int copper;

    /**
     * Larvae consumed.
     */
    @SerializedName("larva_consumed")
    private int larvaConsumed;

    /**
     * Ids of the greenhouse crops the member has fully analyzed, spelled lowercase unlike the
     * uppercase collection and sack item ids.
     */
    @SerializedName("analyzed_greenhouse_crops")
    private @NotNull ConcurrentList<String> analyzedGreenhouseCrops = Concurrent.newList();

    /**
     * Ids of the greenhouse crops the member has seen at least once. A crop must be discovered
     * before it can be analyzed, so this is a superset of the analyzed list rather than a list
     * parallel to it.
     */
    @SerializedName("discovered_greenhouse_crops")
    private @NotNull ConcurrentList<String> discoveredGreenhouseCrops = Concurrent.newList();

    /**
     * The Garden's farming toolkit.
     */
    @SerializedName("farming_toolkit")
    private @NotNull Toolkit farmingToolkit = new Toolkit();

}
