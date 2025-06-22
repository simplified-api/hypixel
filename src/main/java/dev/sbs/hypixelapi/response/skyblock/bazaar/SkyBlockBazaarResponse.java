package dev.sbs.minecraftapi.client.hypixel.response.skyblock.bazaar;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import lombok.Getter;

@Getter
public class SkyBlockBazaarResponse {

    private boolean success;
    private SkyBlockDate.RealTime lastUpdated;
    private final ConcurrentMap<String, SkyBlockBazaarProduct> products = Concurrent.newMap();

}
