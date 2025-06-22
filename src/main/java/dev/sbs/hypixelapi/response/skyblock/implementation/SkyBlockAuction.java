package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.client.hypixel.response.Rarity;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.NbtContent;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class SkyBlockAuction {

    @SerializedName("item_bytes")
    private NbtContent itemNbt;
    @SerializedName("uuid")
    private UUID auctionId;
    @SerializedName("auctioneer")
    private UUID auctioneerId;
    @SerializedName("profile_id")
    private UUID islandId;
    @SerializedName("coop")
    private ConcurrentList<UUID> coopMembers = Concurrent.newList();
    @SerializedName("start")
    private SkyBlockDate.RealTime startedAt;
    @SerializedName("end")
    private SkyBlockDate.RealTime endsAt;
    @Getter(AccessLevel.NONE)
    @SerializedName("item_lore")
    private String lore;
    private String extra;
    @SerializedName("tier")
    private Rarity rarity;
    @SerializedName("starting_big")
    private long startingBid;
    private boolean claimed;
    @SerializedName("claimed_bidders")
    private ConcurrentList<String> claimedBidders = Concurrent.newList();
    @SerializedName("highest_bid_amount")
    private long highestBid;
    private @NotNull ConcurrentList<Bid> bids = Concurrent.newList();
    private boolean bin;

    public @NotNull ConcurrentList<String> getLore() {
        return Concurrent.newUnmodifiableList(StringUtil.split(this.lore, '\n'));
    }

    public boolean notClaimed() {
        return !this.isClaimed();
    }

    @Getter
    public static class Bid {

        @SerializedName("auction_id")
        private UUID auctionId;
        @SerializedName("bidder")
        private UUID bidderId;
        @SerializedName("profile_id")
        private UUID islandId;
        private long amount;
        private SkyBlockDate.RealTime timestamp;

    }

    @Getter
    public static class Ended {

        @SerializedName("auction_id")
        private UUID auctionId;
        @SerializedName("seller")
        private UUID sellerId;
        @SerializedName("seller_profile")
        private UUID sellerIslandId;
        @SerializedName("buyer")
        private UUID buyerId;
        private SkyBlockDate.RealTime timestamp;
        private long price;
        private boolean bin;
        @SerializedName("item_bytes")
        private NbtContent itemNbt;

    }

}
