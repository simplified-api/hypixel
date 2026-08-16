package api.simplified.hypixel.response.resource;

import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.Collection;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

/**
 * The published definitions of every Hypixel SkyBlock collection group and the tier ladders beneath
 * them.
 */
@Getter
public class ResourceCollections {

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
     * SkyBlock version the resource was generated against.
     */
    @SerializedName("version")
    private @NotNull String version;

    /**
     * Every collection group, keyed by group id.
     */
    private @NotNull ConcurrentMap<String, Collection> collections = Concurrent.newMap();

    // TODO: Migrate away from JpaModel

}
