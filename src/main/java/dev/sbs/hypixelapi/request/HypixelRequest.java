package dev.sbs.minecraftapi.client.hypixel.request;

import dev.sbs.api.client.request.IRequest;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelCountsResponse;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelGuildResponse;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelPlayerResponse;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelPunishmentStatsResponse;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelStatusResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.*;
import feign.Param;
import feign.RequestLine;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface HypixelRequest extends IRequest {

    // Hypixel

    @RequestLine("GET /counts")
    @NotNull HypixelCountsResponse getCounts();

    @RequestLine("GET /guild?id={id}")
    @NotNull HypixelGuildResponse getGuild(@Param("id") String guildId);

    @RequestLine("GET /guild?name={name}")
    @NotNull HypixelGuildResponse getGuildByName(@Param("name") String guildName);

    @RequestLine("GET /guild?player={player}")
    @NotNull HypixelGuildResponse getGuildByPlayer(@Param("player") UUID playerId);

    @RequestLine("GET /player?uuid={uuid}")
    @NotNull HypixelPlayerResponse getPlayer(@Param("uuid") UUID playerId);

    @RequestLine("GET /punishmentstats")
    @NotNull HypixelPunishmentStatsResponse getPunishmentStats();

    @RequestLine("GET /status?uuid={uuid}")
    @NotNull HypixelStatusResponse getStatus(@Param("uuid") UUID playerId);

    // SkyBlock

    @RequestLine("GET /skyblock/museum?profile={profile}")
    @NotNull SkyBlockMuseumResponse getMuseum();

    @RequestLine("GET /skyblock/news")
    @NotNull SkyBlockNewsResponse getNews();

    @RequestLine("GET /v2/skyblock/profiles?uuid={uuid}")
    @NotNull SkyBlockProfilesResponse getProfiles(@Param("uuid") UUID uniqueId);

    @RequestLine("GET /skyblock/bazaar")
    @NotNull SkyBlockBazaarResponse getBazaar();

    @RequestLine("GET /skyblock/auction?uuid={uuid}")
    @NotNull SkyBlockAuctionResponse getAuction(@Param("uuid") UUID auctionId);

    @RequestLine("GET /skyblock/auction?profile={profile}")
    @NotNull SkyBlockAuctionResponse getAuctionByIsland(@Param("profile") UUID islandId);

    @RequestLine("GET /skyblock/auction?player={player}")
    @NotNull SkyBlockAuctionResponse getAuctionByPlayer(@Param("player") UUID playerId);

    @RequestLine("GET /skyblock/auctions")
    @NotNull SkyBlockAuctionsResponse getAuctions();

    @RequestLine("GET /skyblock/auctions?page={page}")
    @NotNull SkyBlockAuctionsResponse getAuctions(@Param("page") Integer page);

    @RequestLine("GET /skyblock/auctions_ended")
    @NotNull SkyBlockAuctionsEndedResponse getEndedAuctions();

    @RequestLine("GET /skyblock/firesales")
    @NotNull SkyBlockFireSaleResponse getFireSales();

    // SkyBlock Resources

    @RequestLine("GET /resources/skyblock/skills")
    @NotNull ResourceSkillsResponse getSkills();

    @RequestLine("GET /resources/skyblock/collections")
    @NotNull ResourceCollectionsResponse getCollections();

    @RequestLine("GET /resources/skyblock/items")
    @NotNull ResourceItemsResponse getItems();

    @RequestLine("GET /resources/skyblock/election")
    @NotNull ResourceElectionResponse getElection();


}
