package dev.sbs.minecraftapi.client.hypixel.response.resource;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelGame;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
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
    private boolean retired;
    @SerializedName("lastUpdated")
    private @NotNull SkyBlockDate.RealTime lastUpdated;
    private @NotNull ConcurrentMap<String, HypixelGame> games = Concurrent.newMap();

}
