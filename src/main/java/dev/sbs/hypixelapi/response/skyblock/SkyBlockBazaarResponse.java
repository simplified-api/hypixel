package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.skyblock.SkyBlockBazaarProduct;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.Getter;

/**
 * List of products along with their sell summary, buy summary, and quick status
 */
@Getter
public class SkyBlockBazaarResponse {

    private boolean success;
    private SkyBlockDate.RealTime lastUpdated;
    private final ConcurrentMap<String, SkyBlockBazaarProduct> products = Concurrent.newMap();

}
