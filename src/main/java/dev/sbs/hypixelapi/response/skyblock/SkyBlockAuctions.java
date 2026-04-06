package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Currently active auctions sorted by most recent.
 */
@Getter
public class SkyBlockAuctions {

    private boolean success;
    private int page;
    private int totalPages;
    private int totalAuctions;
    private SkyBlockDate.RealTime lastUpdated;
    private @NotNull ConcurrentList<SkyBlockAuction> auctions = Concurrent.newList();

}
