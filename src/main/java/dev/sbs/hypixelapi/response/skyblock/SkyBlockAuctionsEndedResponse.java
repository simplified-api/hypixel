package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.skyblock.Auction;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class SkyBlockAuctionsEndedResponse {

    private boolean success;
    private SkyBlockDate.RealTime lastUpdated;
    private @NotNull ConcurrentList<Auction.Ended> auctions = Concurrent.newList();

}
