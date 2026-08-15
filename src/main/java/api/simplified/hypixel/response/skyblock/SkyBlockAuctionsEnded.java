package api.simplified.hypixel.response.skyblock;

import api.simplified.skyblock.date.SkyBlockDate;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * The listings that sold within the last sixty seconds.
 * <p>
 * Hypixel keeps no longer window than that, so a caller tracking sale prices over time polls this
 * feed rather than asking for a range.
 */
@Getter
public class SkyBlockAuctionsEnded {

    /**
     * Whether the request was served.
     */
    private boolean success;

    /**
     * When Hypixel last rebuilt the window.
     */
    private SkyBlockDate.RealTime lastUpdated;

    /**
     * The listings that closed inside the window, empty rather than null when none did.
     */
    private @NotNull ConcurrentList<SkyBlockAuction.Ended> auctions = Concurrent.newList();

}
