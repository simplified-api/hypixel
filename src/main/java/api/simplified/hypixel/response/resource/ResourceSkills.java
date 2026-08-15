package api.simplified.hypixel.response.resource;

import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.Skill;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

/**
 * The published definitions of every Hypixel SkyBlock skill and the level ladder each one climbs.
 */
@Getter
public class ResourceSkills {

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
     * Every skill, keyed by skill id.
     */
    private @NotNull ConcurrentMap<String, Skill> skills = Concurrent.newMap();

    // TODO: Migrate away from JpaModel

}
