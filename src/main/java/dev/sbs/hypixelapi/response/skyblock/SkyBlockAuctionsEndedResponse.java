package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.SkyBlockAuction;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class SkyBlockAuctionsEndedResponse {

    private boolean success;
    private SkyBlockDate.RealTime lastUpdated;
    private @NotNull ConcurrentList<SkyBlockAuction.Ended> auctions = Concurrent.newList();

}
