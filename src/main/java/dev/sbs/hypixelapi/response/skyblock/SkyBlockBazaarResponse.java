package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.SkyBlockBazaarProduct;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.Getter;

@Getter
public class SkyBlockBazaarResponse {

    private boolean success;
    private SkyBlockDate.RealTime lastUpdated;
    private final ConcurrentMap<String, SkyBlockBazaarProduct> products = Concurrent.newMap();

}
