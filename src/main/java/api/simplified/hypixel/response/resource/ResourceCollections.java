package api.simplified.hypixel.response.resource;

import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.Collection;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Information regarding Collections.
 */
@Getter
public class ResourceCollections {

    private boolean success;
    @SerializedName("lastUpdated")
    private @NotNull SkyBlockDate.SkyBlockTime lastUpdated;
    @SerializedName("version")
    private @NotNull String version;
    private @NotNull ConcurrentMap<String, Collection> collections = Concurrent.newMap();

    // TODO: Migrate away from JpaModel

}
