package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
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
