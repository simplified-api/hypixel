package dev.sbs.minecraftapi.client.hypixel.response.skyblock.auction;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class SkyBlockAuctionsEndedResponse {

    private boolean success;
    private SkyBlockDate.RealTime lastUpdated;
    private @NotNull ConcurrentList<SkyBlockAuction.Ended> auctions = Concurrent.newList();

}
