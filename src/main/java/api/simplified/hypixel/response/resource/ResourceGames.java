package api.simplified.hypixel.response.resource;

import api.simplified.hypixel.response.hypixel.HypixelGame;
import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * The published definitions of every game on the Hypixel network.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceGames {

    /**
     * Whether the wire reported the request as successful.
     */
    private boolean success;

    /**
     * Whether the resource carries the retired flag.
     */
    private boolean retired;

    /**
     * When the resource was last regenerated.
     */
    @SerializedName("lastUpdated")
    private @NotNull SkyBlockDate.RealTime lastUpdated;

    /**
     * Every game, keyed by the game's type name.
     */
    private @NotNull ConcurrentMap<String, HypixelGame> games = Concurrent.newMap();

}
