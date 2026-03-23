package dev.sbs.minecraftapi.client.hypixel.request;

import dev.sbs.api.client.request.Endpoint;
import dev.sbs.api.client.route.Route;
import dev.sbs.minecraftapi.client.hypixel.exception.HypixelApiException;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelCounts;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelGuildResponse;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelPlayerResponse;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelPunishmentStats;
import dev.sbs.minecraftapi.client.hypixel.response.hypixel.HypixelStatus;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceCollections;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceElection;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceGames;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceItems;
import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceSkills;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuctionResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuctions;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockAuctionsEnded;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockBazaar;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockFireSaleResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockGardenResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockMuseumResponse;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockNews;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.SkyBlockProfiles;
import feign.Param;
import feign.RequestLine;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an interface for interacting with the Hypixel API endpoint.
 * <p>
 * This interface defines a set of methods that correspond to various Hypixel API endpoints
 * for fetching information about players, guilds, SkyBlock data, and more.
 * <p>
 * Many endpoints require an API key for authentication, as noted in the corresponding
 * {@code @apiNote} for the individual methods.
 *
 * @see <a href="https://api.hypixel.net/">Hypixel API</a>
 * @version v2
 */
@Route("api.hypixel.net/v2")
public interface HypixelEndpoint extends Endpoint {

    // Hypixel

    /**
     * Request the current player counts for Hypixel games.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
     */
    @RequestLine("GET /counts")
    @NotNull HypixelCounts getCounts() throws HypixelApiException;

    /**
     * Request a guild by id.
     *
     * @param guildId The id of the guild to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
     */
    @RequestLine("GET /guild?id={id}")
    @NotNull HypixelGuildResponse getGuildById(@Param("id") String guildId) throws HypixelApiException;

    /**
     * Request a guild by name.
     *
     * @param guildName The name of the guild to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
     */
    @RequestLine("GET /guild?name={name}")
    @NotNull HypixelGuildResponse getGuildByName(@Param("name") String guildName) throws HypixelApiException;

    /**
     * Request the guild of a specific player.
     *
     * @param playerId The UUID of the player to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
     */
    @RequestLine("GET /guild?player={player}")
    @NotNull HypixelGuildResponse getGuildByPlayer(@Param("player") UUID playerId) throws HypixelApiException;

    /**
     * Request the data and game stats of a specific player.
     *
     * @param playerId The UUID of the player to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
     */
    @RequestLine("GET /player?uuid={uuid}")
    @NotNull HypixelPlayerResponse getPlayer(@Param("uuid") UUID playerId) throws HypixelApiException;

    /**
     * Request the network punishment statistics.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
     */
    @RequestLine("GET /punishmentstats")
    @NotNull HypixelPunishmentStats getPunishmentStats() throws HypixelApiException;

    /**
     * Request the current online status of a specific player.
     *
     * @param playerId The UUID of the player to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
     */
    @RequestLine("GET /status?uuid={uuid}")
    @NotNull HypixelStatus getStatus(@Param("uuid") UUID playerId) throws HypixelApiException;

    // Hypixel Resources

    /**
     * Request information about all games.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /resources/games")
    @NotNull ResourceGames getGames() throws HypixelApiException;

    // SkyBlock

    /**
     * Request the Museum data for all members of the provided profile.
     * <p>
     * The data returned can differ depending on the players' in-game API settings.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
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
     * @apiNote API-Key Required
     */
    @RequestLine("GET /skyblock/news")
    @NotNull SkyBlockNews getNews() throws HypixelApiException;

    /**
     * Request all profiles of a player. This includes skills, collections, stats, objectives, etc.
     * <p>
     * The data returned can differ depending on the players' in-game API settings.
     *
     * @param playerId The UUID of the player to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
     */
    @RequestLine("GET /skyblock/profiles?uuid={uuid}")
    @NotNull SkyBlockProfiles getProfiles(@Param("uuid") UUID playerId) throws HypixelApiException;

    /**
     * Request the list of products along with their sell summary, buy summary, and quick status.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /skyblock/bazaar")
    @NotNull SkyBlockBazaar getBazaar() throws HypixelApiException;

    /**
     * Request a specific Auction.
     *
     * @param auctionId The UUID of the auction to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
     */
    @RequestLine("GET /skyblock/auction?uuid={uuid}")
    @NotNull SkyBlockAuctionResponse getAuctionById(@Param("uuid") UUID auctionId) throws HypixelApiException;

    /**
     * Request all Auctions of a specific island.
     *
     * @param islandId The UUID of the island to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
     */
    @RequestLine("GET /skyblock/auction?profile={profile}")
    @NotNull SkyBlockAuctionResponse getAuctionByIsland(@Param("profile") UUID islandId) throws HypixelApiException;

    /**
     * Request all Auctions of a specific player.
     *
     * @param playerId The UUID of the player to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Required
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
    @NotNull SkyBlockAuctions getAuctions() throws HypixelApiException;

    /**
     * Request the currently active auctions sorted by most recent and paginated.
     *
     * @param page The page number to request.
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /skyblock/auctions?page={page}")
    @NotNull SkyBlockAuctions getAuctions(@Param("page") Integer page) throws HypixelApiException;

    /**
     * Request auctions which ended in the last 60 seconds.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /skyblock/auctions_ended")
    @NotNull SkyBlockAuctionsEnded getEndedAuctions() throws HypixelApiException;

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
     * @apiNote API-Key Required
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
    @NotNull ResourceSkills getSkills() throws HypixelApiException;

    /**
     * Request information regarding Collections.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /resources/skyblock/collections")
    @NotNull ResourceCollections getCollections() throws HypixelApiException;

    /**
     * Request information regarding Items.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /resources/skyblock/items")
    @NotNull ResourceItems getItems() throws HypixelApiException;

    /**
     * Request information regarding the current mayor and ongoing election.
     *
     * @throws HypixelApiException Thrown when the Hypixel Api encounters an error in the HTTP
     * status range of 400+.
     * @apiNote API-Key Not Required
     */
    @RequestLine("GET /resources/skyblock/election")
    @NotNull ResourceElection getElection() throws HypixelApiException;

}
