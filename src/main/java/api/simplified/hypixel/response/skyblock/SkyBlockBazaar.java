package api.simplified.hypixel.response.skyblock;

import api.simplified.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;

/**
 * A snapshot of every product traded on the Bazaar, each with both sides of its order book.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Bazaar">Bazaar</a>
 */
@Getter
public class SkyBlockBazaar {

    /**
     * Whether the request was served.
     */
    private boolean success;

    /**
     * When Hypixel last rebuilt the snapshot.
     */
    private SkyBlockDate.RealTime lastUpdated;

    /**
     * Every tradeable product keyed by its item id.
     */
    private final ConcurrentMap<String, SkyBlockProduct> products = Concurrent.newMap();

}
