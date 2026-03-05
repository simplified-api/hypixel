package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Auction(s) specific to an ID, Player, or Island.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SkyBlockAuctionResponse {

    private boolean success;
    private @NotNull ConcurrentList<SkyBlockAuction> auctions = Concurrent.newList();

}
