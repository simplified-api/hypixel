package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.SerializedPath;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class BestiaryData {

    private @NotNull ConcurrentMap<String, Integer> kills = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Integer> deaths = Concurrent.newMap();
    @SerializedPath("milestone.last_claimed_milestone")
    private int lastClaimedMilestone;

}