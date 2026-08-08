package api.simplified.hypixel.response.resource;

import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.Item;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Information regarding Items.
 */
@Getter
public class ResourceItems {

    private boolean success;
    @SerializedName("lastUpdated")
    private @NotNull SkyBlockDate.RealTime lastUpdated;
    private @NotNull ConcurrentList<Item> items = Concurrent.newList();

    // TODO: Migrate away from JpaModel

}
