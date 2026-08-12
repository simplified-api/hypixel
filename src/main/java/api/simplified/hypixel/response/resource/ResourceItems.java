package api.simplified.hypixel.response.resource;

import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.Item;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * The published definitions of every Hypixel SkyBlock item.
 */
@Getter
public class ResourceItems {

    /**
     * Whether the wire reported the request as successful.
     */
    private boolean success;

    /**
     * When the resource was last regenerated.
     */
    @SerializedName("lastUpdated")
    private @NotNull SkyBlockDate.RealTime lastUpdated;

    /**
     * Every item the resource publishes, in wire order.
     */
    private @NotNull ConcurrentList<Item> items = Concurrent.newList();

    // TODO: Migrate away from JpaModel

}
