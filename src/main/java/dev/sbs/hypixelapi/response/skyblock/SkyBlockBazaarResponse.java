package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.skyblock.BazaarProduct;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.Getter;

@Getter
public class SkyBlockBazaarResponse {

    private boolean success;
    private SkyBlockDate.RealTime lastUpdated;
    private final ConcurrentMap<String, BazaarProduct> products = Concurrent.newMap();

}
