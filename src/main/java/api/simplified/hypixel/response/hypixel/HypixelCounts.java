package api.simplified.hypixel.response.hypixel;

import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Live player counts for the Hypixel network, broken down by game and by mode within each game.
 */
@Getter
public class HypixelCounts {

    /**
     * Whether the wire reported the request as successful.
     */
    private boolean success;

    /**
     * Total players online across the whole network.
     */
    private int playerCount;

    /**
     * Player counts per game, keyed by the game's type name.
     */
    private @NotNull ConcurrentMap<String, Game> games = Concurrent.newMap();

    /**
     * Player counts for one game and for each of its modes.
     */
    @Getter
    public static class Game {

        /**
         * Total players online in this game across every mode.
         */
        private int players;

        /**
         * Player counts per mode, keyed by the mode's wire name.
         */
        private @NotNull ConcurrentMap<String, Integer> modes = Concurrent.newMap();

    }

}
