package dev.sbs.minecraftapi.client.hypixel.request;

import dev.sbs.api.client.request.Endpoint;
import dev.sbs.api.client.route.Route;
import dev.sbs.minecraftapi.client.hypixel.HypixelClient;
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
 * Feign endpoint interface for the Hypixel Public API v2.
 * <p>
 * Most endpoints require authentication via the {@code API-Key} header, which
 * is supplied automatically by the {@link HypixelClient}'s dynamic headers.
 * Resource endpoints under {@code /resources/} are publicly accessible without
 * authentication.
 * <p>
 * Rate limits are enforced per API key. The server responds with
 * {@code RateLimit-Limit}, {@code RateLimit-Remaining}, and
 * {@code RateLimit-Reset} headers, which are tracked by the underlying
 * {@link dev.sbs.api.client.ratelimit.RateLimitManager RateLimitManager}.
 *
 * @see HypixelClient
 * @see <a href="https://api.hypixel.net/">Hypixel Public API</a>
 */
@Route("api.hypixel.net/v2")
public interface HypixelEndpoint extends Endpoint {

    // ---- Hypixel ----

    /**
     * Fetches the current player counts for all Hypixel games.
     *
     * @return the current player count data
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /counts")
    @NotNull HypixelCounts getCounts() throws HypixelApiException;

    /**
     * Fetches a guild by its guild id.
     *
     * @param guildId the guild id
     * @return the guild response
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /guild?id={id}")
    @NotNull HypixelGuildResponse getGuildById(@Param("id") String guildId) throws HypixelApiException;

    /**
     * Fetches a guild by its name.
     *
     * @param guildName the guild name
     * @return the guild response
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /guild?name={name}")
    @NotNull HypixelGuildResponse getGuildByName(@Param("name") String guildName) throws HypixelApiException;

    /**
     * Fetches the guild that a player belongs to.
     *
     * @param playerId the player's unique id
     * @return the guild response
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /guild?player={player}")
    @NotNull HypixelGuildResponse getGuildByPlayer(@Param("player") UUID playerId) throws HypixelApiException;

    /**
     * Fetches the data and game stats of a specific player.
     *
     * @param playerId the player's unique id
     * @return the player data response
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /player?uuid={uuid}")
    @NotNull HypixelPlayerResponse getPlayer(@Param("uuid") UUID playerId) throws HypixelApiException;

    /**
     * Fetches the network-wide punishment statistics.
     *
     * @return the punishment statistics
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /punishmentstats")
    @NotNull HypixelPunishmentStats getPunishmentStats() throws HypixelApiException;

    /**
     * Fetches the current online status of a specific player.
     *
     * @param playerId the player's unique id
     * @return the player's online status
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /status?uuid={uuid}")
    @NotNull HypixelStatus getStatus(@Param("uuid") UUID playerId) throws HypixelApiException;

    // ---- Resources ----

    /**
     * Fetches information about all Hypixel games. Does not require an API key.
     *
     * @return the game information
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /resources/games")
    @NotNull ResourceGames getGames() throws HypixelApiException;

    // ---- SkyBlock ----

    /**
     * Fetches museum data for all members of the given SkyBlock profile.
     * <p>
     * The data returned may vary depending on each player's in-game API settings.
     *
     * @param islandId the SkyBlock profile id
     * @return the museum data response
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/museum?profile={profile}")
    @NotNull SkyBlockMuseumResponse getMuseum(@Param("profile") UUID islandId) throws HypixelApiException;

    /**
     * Fetches recent SkyBlock news and announcements.
     *
     * @return the news articles
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/news")
    @NotNull SkyBlockNews getNews() throws HypixelApiException;

    /**
     * Fetches all SkyBlock profiles for the given player, including skills,
     * collections, stats, and objectives.
     * <p>
     * The data returned may vary depending on each player's in-game API settings.
     *
     * @param playerId the player's unique id
     * @return the profiles response
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/profiles?uuid={uuid}")
    @NotNull SkyBlockProfiles getProfiles(@Param("uuid") UUID playerId) throws HypixelApiException;

    /**
     * Fetches all Bazaar products with their sell summary, buy summary, and
     * quick status. Does not require an API key.
     *
     * @return the bazaar product data
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/bazaar")
    @NotNull SkyBlockBazaar getBazaar() throws HypixelApiException;

    /**
     * Fetches a specific auction by its auction id.
     *
     * @param auctionId the auction's unique id
     * @return the auction response
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/auction?uuid={uuid}")
    @NotNull SkyBlockAuctionResponse getAuctionById(@Param("uuid") UUID auctionId) throws HypixelApiException;

    /**
     * Fetches all auctions belonging to the given SkyBlock profile.
     *
     * @param islandId the SkyBlock profile id
     * @return the auction response
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/auction?profile={profile}")
    @NotNull SkyBlockAuctionResponse getAuctionByIsland(@Param("profile") UUID islandId) throws HypixelApiException;

    /**
     * Fetches all auctions created by the given player.
     *
     * @param playerId the player's unique id
     * @return the auction response
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/auction?player={player}")
    @NotNull SkyBlockAuctionResponse getAuctionByPlayer(@Param("player") UUID playerId) throws HypixelApiException;

    /**
     * Fetches all currently active auctions, sorted by most recently updated
     * and paginated. Does not require an API key.
     *
     * @return the first page of active auctions
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/auctions")
    @NotNull SkyBlockAuctions getAuctions() throws HypixelApiException;

    /**
     * Fetches a specific page of currently active auctions, sorted by most
     * recently updated. Does not require an API key.
     *
     * @param page the zero-based page number
     * @return the requested page of active auctions
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/auctions?page={page}")
    @NotNull SkyBlockAuctions getAuctions(@Param("page") Integer page) throws HypixelApiException;

    /**
     * Fetches auctions that ended within the last 60 seconds. Does not require
     * an API key.
     *
     * @return the recently ended auctions
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/auctions_ended")
    @NotNull SkyBlockAuctionsEnded getEndedAuctions() throws HypixelApiException;

    /**
     * Fetches the currently active or upcoming SkyBlock Fire Sales. Does not
     * require an API key.
     *
     * @return the fire sale data
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/firesales")
    @NotNull SkyBlockFireSaleResponse getFireSales() throws HypixelApiException;

    /**
     * Fetches garden data for the given SkyBlock profile.
     *
     * @param islandId the SkyBlock profile id
     * @return the garden data response
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /skyblock/garden?profile={profile}")
    @NotNull SkyBlockGardenResponse getGarden(@Param("profile") UUID islandId) throws HypixelApiException;

    // ---- SkyBlock Resources ----

    /**
     * Fetches SkyBlock skill definitions and leveling data. Does not require an
     * API key.
     *
     * @return the skill information
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /resources/skyblock/skills")
    @NotNull ResourceSkills getSkills() throws HypixelApiException;

    /**
     * Fetches SkyBlock collection definitions and tier data. Does not require an
     * API key.
     *
     * @return the collection information
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /resources/skyblock/collections")
    @NotNull ResourceCollections getCollections() throws HypixelApiException;

    /**
     * Fetches SkyBlock item definitions. Does not require an API key.
     *
     * @return the item information
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /resources/skyblock/items")
    @NotNull ResourceItems getItems() throws HypixelApiException;

    /**
     * Fetches the current SkyBlock mayor and ongoing election data. Does not
     * require an API key.
     *
     * @return the election and mayor information
     * @throws HypixelApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /resources/skyblock/election")
    @NotNull ResourceElection getElection() throws HypixelApiException;

}
