package api.simplified.hypixel.response.skyblock;

import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * The listings a single auction lookup returned, whether it was made by auction id, by player or by
 * profile.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SkyBlockAuctionResponse {

    /**
     * Whether the request was served.
     */
    private boolean success;

    /**
     * The listings that matched, empty rather than null when the lookup found none.
     */
    private @NotNull ConcurrentList<SkyBlockAuction> auctions = Concurrent.newList();

}
