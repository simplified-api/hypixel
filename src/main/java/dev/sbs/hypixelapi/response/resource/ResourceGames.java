package dev.sbs.minecraftapi.client.hypixel.response.resource;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelGame;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Information regarding Games.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceGames {

    private boolean success;
    private long lastUpdated;
    private boolean retired;
    private @NotNull ConcurrentList<HypixelGame> games = Concurrent.newList();

}
