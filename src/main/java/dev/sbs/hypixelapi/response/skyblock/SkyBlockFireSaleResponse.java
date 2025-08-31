package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.skyblock.FireSale;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class SkyBlockFireSaleResponse {

    private boolean success;
    private @NotNull ConcurrentList<FireSale> sales = Concurrent.newList();

}
