package api.simplified.hypixel.response.hypixel;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

/**
 * One game on the Hypixel network, as the games resource describes it.
 */
@Getter
public class HypixelGame {

    /**
     * Numeric id the network assigns the game.
     */
    private int id;

    /**
     * Internal database name for the game, which is not always its display name.
     */
    @SerializedName("databaseName")
    private @NotNull String databaseName;

    /**
     * Display name of the game.
     */
    @SerializedName("name")
    private @NotNull String name;

    /**
     * Whether the game is flagged as legacy.
     */
    private boolean legacy;

    /**
     * Whether the game is flagged as retired and no longer playable.
     */
    private boolean retired;

    /**
     * Display names for the game's modes, keyed by the mode's own name.
     */
    private @NotNull ConcurrentMap<String, String> modeNames = Concurrent.newMap();

}
