package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.skyblock.SkyBlockFireSale;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Currently active or upcoming Fire Sales.
 */
@Getter
public class SkyBlockFireSaleResponse {

    private boolean success;
    private @NotNull ConcurrentList<SkyBlockFireSale> sales = Concurrent.newList();

}
