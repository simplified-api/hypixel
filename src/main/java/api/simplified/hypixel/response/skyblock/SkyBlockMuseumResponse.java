package api.simplified.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * The museums of every member of one profile, looked up by profile id.
 */
@Getter
public class SkyBlockMuseumResponse {

    /**
     * Whether the request was served.
     */
    private boolean success;

    /**
     * Each member's museum keyed by their unique id, missing a member who has hidden theirs through
     * their in-game API settings.
     */
    @SerializedName("members")
    private @NotNull ConcurrentMap<UUID, SkyBlockMuseum> members = Concurrent.newMap();

}
