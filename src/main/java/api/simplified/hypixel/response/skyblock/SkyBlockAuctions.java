package api.simplified.hypixel.response.skyblock;

import api.simplified.skyblock.date.SkyBlockDate;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * One page of the currently active listings, ordered by the most recently updated.
 * <p>
 * The whole Auction House is paginated rather than sent at once, so a caller walking every listing
 * reads pages zero through {@link #getTotalPages()} less one.
 */
@Getter
public class SkyBlockAuctions {

    /**
     * Whether the request was served.
     */
    private boolean success;

    /**
     * Zero-based number of the page this response carries.
     */
    private int page;

    /**
     * Pages the Auction House currently spans.
     */
    private int totalPages;

    /**
     * Listings active across every page.
     */
    private int totalAuctions;

    /**
     * When Hypixel last rebuilt the paginated snapshot.
     */
    private SkyBlockDate.RealTime lastUpdated;

    /**
     * The listings on this page, empty rather than null past the last page.
     */
    private @NotNull ConcurrentList<SkyBlockAuction> auctions = Concurrent.newList();

}
