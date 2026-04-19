package dev.sbs.hypixelapi.response.resource;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.model.Collection;
import dev.sbs.skyblockdata.date.SkyBlockDate;
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
