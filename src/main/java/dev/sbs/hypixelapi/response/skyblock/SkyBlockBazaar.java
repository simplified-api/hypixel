package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;

/**
 * List of products along with their sell summary, buy summary, and quick status
 */
@Getter
public class SkyBlockBazaar {

    private boolean success;
    private SkyBlockDate.RealTime lastUpdated;
    private final ConcurrentMap<String, SkyBlockProduct> products = Concurrent.newMap();

}
