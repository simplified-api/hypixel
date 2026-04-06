package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Auctions which ended in the last 60 seconds.
 */
@Getter
public class SkyBlockAuctionsEnded {

    private boolean success;
    private SkyBlockDate.RealTime lastUpdated;
    private @NotNull ConcurrentList<SkyBlockAuction.Ended> auctions = Concurrent.newList();

}
