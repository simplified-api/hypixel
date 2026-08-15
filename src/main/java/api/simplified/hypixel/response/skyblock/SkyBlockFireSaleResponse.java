package api.simplified.hypixel.response.skyblock;

import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * The fire sales that are running or announced.
 */
@Getter
public class SkyBlockFireSaleResponse {

    /**
     * Whether the request was served.
     */
    private boolean success;

    /**
     * The sales on offer, empty rather than null between events.
     */
    private @NotNull ConcurrentList<SkyBlockFireSale> sales = Concurrent.newList();

}
