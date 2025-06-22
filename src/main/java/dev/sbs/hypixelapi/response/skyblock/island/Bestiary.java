package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.SerializedPath;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Bestiary {

    private @NotNull ConcurrentMap<String, Integer> kills = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Integer> deaths = Concurrent.newMap();
    @SerializedPath("milestone.last_claimed_milestone")
    private int lastClaimedMilestone;

}
