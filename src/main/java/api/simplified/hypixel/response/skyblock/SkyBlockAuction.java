package api.simplified.hypixel.response.skyblock;

import api.simplified.hypixel.common.NbtContent;
import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.util.StringUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * One listing on the Auction House, either a bidding auction or a buy-it-now sale.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Auction_House">Auction House</a>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SkyBlockAuction {

    /**
     * The item on offer, carried as base64 NBT rather than as JSON.
     */
    @SerializedName("item_bytes")
    private @NotNull NbtContent item = new NbtContent();

    /**
     * Unique id of the listing itself.
     */
    @SerializedName("uuid")
    private UUID auctionId;

    /**
     * Unique id of the player who put the item up.
     */
    @SerializedName("auctioneer")
    private UUID auctioneerId;

    /**
     * Unique id of the profile the item was listed from.
     */
    @SerializedName("profile_id")
    private UUID islandId;

    /**
     * Unique ids of the co-op members who share the listing with the auctioneer.
     */
    @SerializedName("coop")
    private @NotNull ConcurrentList<UUID> coopMembers = Concurrent.newList();

    /**
     * When the listing opened.
     */
    @SerializedName("start")
    private SkyBlockDate.RealTime startedAt;

    /**
     * When the listing closes, or closed.
     */
    @SerializedName("end")
    private SkyBlockDate.RealTime endsAt;

    /**
     * The item's lore as the single newline-joined string the wire sends, split by {@link #getLore()}.
     */
    @Getter(AccessLevel.NONE)
    @SerializedName("item_lore")
    private @NotNull String lore = "";

    /**
     * Free text Hypixel builds from the item's name and lore for the Auction House search.
     */
    private String extra;

    /**
     * Rarity of the item on offer, {@link Rarity#COMMON} when the wire names no tier.
     */
    @SerializedName("tier")
    private @NotNull Rarity rarity = Rarity.COMMON;

    /**
     * Coins the listing opened at, which is the whole price of a buy-it-now sale.
     */
    @SerializedName("starting_bid")
    private long startingBid;

    /**
     * Whether the seller has collected the sale.
     */
    private boolean claimed;

    /**
     * Outbid players who have already collected their coins back.
     */
    @SerializedName("claimed_bidders")
    private @NotNull ConcurrentList<String> claimedBidders = Concurrent.newList();

    /**
     * Coins of the leading bid, zero while nobody has bid.
     */
    @SerializedName("highest_bid_amount")
    private long highestBid;

    /**
     * Every bid placed on the listing.
     */
    private @NotNull ConcurrentList<Bid> bids = Concurrent.newList();

    /**
     * Whether the listing sells outright at its starting price rather than taking bids.
     */
    private boolean bin;

    /**
     * The item's lore split into its lines, a fresh unmodifiable list on every call.
     */
    public @NotNull ConcurrentList<String> getLore() {
        return Concurrent.newUnmodifiableList(StringUtil.split(this.lore, '\n'));
    }

    /**
     * Whether the sale is still waiting to be collected, the inverse of {@link #isClaimed()}.
     */
    public boolean notClaimed() {
        return !this.isClaimed();
    }

    /**
     * A single bid placed against a listing.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Auction_House">Auction House</a>
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Bid {

        /**
         * Unique id of the listing the bid was placed on.
         */
        @SerializedName("auction_id")
        private UUID auctionId;

        /**
         * Unique id of the player who placed the bid.
         */
        @SerializedName("bidder")
        private UUID bidderId;

        /**
         * Unique id of the profile the bidder was playing.
         */
        @SerializedName("profile_id")
        private UUID islandId;

        /**
         * Coins bid.
         */
        private long amount;

        /**
         * When the bid was placed.
         */
        private SkyBlockDate.RealTime timestamp;

    }

    /**
     * A listing that has already sold, as the recently ended feed reports it.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Auction_House">Auction House</a>
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Ended {

        /**
         * Unique id of the listing that closed.
         */
        @SerializedName("auction_id")
        private UUID auctionId;

        /**
         * Unique id of the player who sold the item.
         */
        @SerializedName("seller")
        private UUID sellerId;

        /**
         * Unique id of the profile the item was listed from.
         */
        @SerializedName("seller_profile")
        private UUID sellerIslandId;

        /**
         * Unique id of the player who bought the item.
         */
        @SerializedName("buyer")
        private UUID buyerId;

        /**
         * When the listing closed.
         */
        private SkyBlockDate.RealTime timestamp;

        /**
         * Coins the item sold for.
         */
        private long price;

        /**
         * Whether the item sold outright rather than to the highest bidder.
         */
        private boolean bin;

        /**
         * The item that changed hands, carried as base64 NBT rather than as JSON.
         */
        @SerializedName("item_bytes")
        private NbtContent item = new NbtContent();

    }

}
