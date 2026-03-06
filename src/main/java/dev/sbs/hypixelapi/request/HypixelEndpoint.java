package dev.sbs.minecraftapi.client.hypixel.request;

import dev.sbs.api.client.request.Endpoint;
import dev.sbs.api.client.route.Route;
import dev.sbs.minecraftapi.client.hypixel.exception.HypixelApiException;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelCountsResponse;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelGuildResponse;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelPlayerResponse;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelPunishmentStatsResponse;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelStatusResponse;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceCollectionsResponse;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceElectionResponse;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceItemsResponse;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceSkillsResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuctionResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuctionsEndedResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuctionsResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockBazaarResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockFireSaleResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockGardenResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockMuseumResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockNewsResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockProfilesResponse;
import feign.Param;
import feign.RequestLine;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Route("api.hypixel.net/v2")
public interface HypixelEndpoint extends Endpoint {

    // Hypixel

    /**
     * Request the current player counts for Hypixel games.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     */
    @RequestLine("GET /counts")
    @NotNull HypixelCountsResponse getCounts() throws HypixelApiException;

    @RequestLine("GET /guild?id={id}")
    @NotNull HypixelGuildResponse getGuildById(@Param("id") String guildId) throws HypixelApiException;

    @RequestLine("GET /guild?name={name}")
    @NotNull HypixelGuildResponse getGuildByName(@Param("name") String guildName) throws HypixelApiException;

    @RequestLine("GET /guild?player={player}")
    @NotNull HypixelGuildResponse getGuildByPlayer(@Param("player") UUID playerId) throws HypixelApiException;

    @RequestLine("GET /player?uuid={uuid}")
    @NotNull HypixelPlayerResponse getPlayer(@Param("uuid") UUID playerId) throws HypixelApiException;

    @RequestLine("GET /punishmentstats")
    @NotNull HypixelPunishmentStatsResponse getPunishmentStats() throws HypixelApiException;

    /**
     * Request the current online status of a specific player.
     *
     * @param playerId The UUID of the player to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     */
    @RequestLine("GET /status?uuid={uuid}")
    @NotNull HypixelStatusResponse getStatus(@Param("uuid") UUID playerId) throws HypixelApiException;

    // SkyBlock

    /**
     * Request the Museum data for all members of the provided profile.
     * <p>
     * The data returned can differ depending on the players' in-game API settings.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     */
    @RequestLine("GET /skyblock/museum?profile={profile}")
    @NotNull SkyBlockMuseumResponse getMuseum() throws HypixelApiException;

    /**
     * Request recent News and Announcements focused on SkyBlock.
     * <p>
     * This does not include Patch Notes or other announcements.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     */
    @RequestLine("GET /skyblock/news")
    @NotNull SkyBlockNewsResponse getNews() throws HypixelApiException;

    /**
     * Request all profiles of a player. This includes skills, collections, stats, objectives, etc.
     * <p>
     * The data returned can differ depending on the players' in-game API settings.
     *
     * @param playerId The UUID of the player to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     */
    @RequestLine("GET /skyblock/profiles?uuid={uuid}")
    @NotNull SkyBlockProfilesResponse getProfiles(@Param("uuid") UUID playerId) throws HypixelApiException;

    /**
     * Request the list of products along with their sell summary, buy summary, and quick status.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /skyblock/bazaar")
    @NotNull SkyBlockBazaarResponse getBazaar() throws HypixelApiException;

    /**
     * Request a specific Auction.
     *
     * @param auctionId The UUID of the auction to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     */
    @RequestLine("GET /skyblock/auction?uuid={uuid}")
    @NotNull SkyBlockAuctionResponse getAuctionById(@Param("uuid") UUID auctionId) throws HypixelApiException;

    /**
     * Request all Auctions of a specific island.
     *
     * @param islandId The UUID of the island to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     */
    @RequestLine("GET /skyblock/auction?profile={profile}")
    @NotNull SkyBlockAuctionResponse getAuctionByIsland(@Param("profile") UUID islandId) throws HypixelApiException;

    /**
     * Request all Auctions of a specific player.
     *
     * @param playerId The UUID of the player to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     */
    @RequestLine("GET /skyblock/auction?player={player}")
    @NotNull SkyBlockAuctionResponse getAuctionByPlayer(@Param("player") UUID playerId) throws HypixelApiException;

    /**
     * Request the currently active auctions sorted by most recent and paginated.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /skyblock/auctions")
    @NotNull SkyBlockAuctionsResponse getAuctions() throws HypixelApiException;

    /**
     * Request the currently active auctions sorted by most recent and paginated.
     *
     * @param page The page number to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /skyblock/auctions?page={page}")
    @NotNull SkyBlockAuctionsResponse getAuctions(@Param("page") Integer page) throws HypixelApiException;

    /**
     * Request auctions which ended in the last 60 seconds.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /skyblock/auctions_ended")
    @NotNull SkyBlockAuctionsEndedResponse getEndedAuctions() throws HypixelApiException;

    /**
     * Request the currently active or upcoming Fire Sales.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /skyblock/firesales")
    @NotNull SkyBlockFireSaleResponse getFireSales() throws HypixelApiException;

    /**
     * Request the garden data for the provided profile.
     *
     * @param islandId The UUID of the island to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     */
    @RequestLine("GET /skyblock/garden?profile={profile}")
    @NotNull SkyBlockGardenResponse getGarden(@Param("profile") UUID islandId) throws HypixelApiException;

    // SkyBlock Resources

    /**
     * Request information regarding Skills.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /resources/skyblock/skills")
    @NotNull ResourceSkillsResponse getSkills() throws HypixelApiException;

    /**
     * Request information regarding Collections.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /resources/skyblock/collections")
    @NotNull ResourceCollectionsResponse getCollections() throws HypixelApiException;

    /**
     * Request information regarding Items.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /resources/skyblock/items")
    @NotNull ResourceItemsResponse getItems() throws HypixelApiException;

    /**
     * Request information regarding the current mayor and ongoing election.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /resources/skyblock/election")
    @NotNull ResourceElectionResponse getElection() throws HypixelApiException;

}
