package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class GardenCore {

    private int copper;
    @SerializedName( "larva_consumed")
    private int larvaConsumed;
    @SerializedName("analyzed_greenhouse_crops")
    private @NotNull ConcurrentList<String> analyzedGreenhouseCrops = Concurrent.newList();
    @SerializedName("discovered_greenhouse_crops")
    private @NotNull ConcurrentList<String> discoveredGreenhouseCrops = Concurrent.newList();


}
