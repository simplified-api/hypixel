package dev.sbs.minecraftapi.client.hypixel.response.hypixel;

import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Current player counts for Hypixel games.
 */
@Getter
public class HypixelCounts {

    private boolean success;
    private int playerCount;
    private @NotNull ConcurrentMap<String, Game> games = Concurrent.newMap();

    @Getter
    public static class Game {

        private int players;
        private @NotNull ConcurrentMap<String, Integer> modes = Concurrent.newMap();

    }

}
