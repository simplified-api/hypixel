package dev.sbs.minecraftapi.client.hypixel;

import dev.sbs.minecraftapi.client.hypixel.response.forum.HypixelForum;
import dev.simplified.client.exception.ApiException;
import dev.simplified.client.request.Contract;
import dev.simplified.client.route.Route;
import feign.Param;
import feign.RequestLine;
import org.jetbrains.annotations.NotNull;

/**
 * Feign contract interface for the public XenForo-generated RSS feeds served by
 * {@code hypixel.net}, covering both the standard {@code /forums/<slug>} sections and the
 * path-less {@code /<slug>} sections that do not sit under {@code /forums/}.
 * <p>
 * All responses are RSS 2.0 XML documents, decoded into {@link HypixelForum} by the
 * {@link dev.simplified.client.codec.XmlDecoder XmlDecoder} installed on the underlying client.
 * A tree transformer configured on that decoder folds the {@code <link>} / {@code <atom:link>}
 * namespace collision before Gson binding, so the DTO exposes a scalar {@code link} field.
 *
 * @see HypixelForum
 * @see <a href="https://hypixel.net/forums/news-and-announcements.4/-/index.rss">Example feed</a>
 */
@Route("hypixel.net")
public interface HypixelForumContract extends Contract {

    /** Forum slug for SkyBlock patch notes: {@code skyblock-patch-notes.158}. */
    @NotNull String SKYBLOCK_PATCH_NOTES = "skyblock-patch-notes.158";

    /** Forum slug for SkyBlock general discussion: {@code skyblock-general-discussion.157}. */
    @NotNull String SKYBLOCK_GENERAL_DISCUSSION = "skyblock-general-discussion.157";

    /** Forum slug for news and announcements: {@code news-and-announcements.4}. */
    @NotNull String NEWS_AND_ANNOUNCEMENTS = "news-and-announcements.4";

    /** Top-level slug for the SkyBlock Alpha section: {@code skyblock-alpha} (served outside {@code /forums/}). */
    @NotNull String SKYBLOCK_ALPHA = "skyblock-alpha";

    /**
     * Fetches the RSS feed for a forum section served under {@code /forums/}.
     *
     * @param identifier the XenForo forum slug including its numeric suffix (e.g. {@code "news-and-announcements.4"})
     * @return the decoded forum feed
     * @throws ApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /forums/{identifier}/-/index.rss")
    @NotNull HypixelForum getForumFeed(@NotNull @Param("identifier") String identifier) throws ApiException;

    /**
     * Fetches the RSS feed for a section served directly under the root, without a
     * {@code /forums/} prefix. Used by the SkyBlock Alpha section and other top-level
     * XenForo nodes that are not nested under the forums tree.
     *
     * @param identifier the top-level XenForo section slug (e.g. {@code "skyblock-alpha"})
     * @return the decoded forum feed
     * @throws ApiException if the server responds with an HTTP status of 400 or higher
     */
    @RequestLine("GET /{identifier}/-/index.rss")
    @NotNull HypixelForum getRootFeed(@NotNull @Param("identifier") String identifier) throws ApiException;

    /**
     * Fetches the RSS feed for the SkyBlock patch notes forum section.
     *
     * @return the decoded forum feed
     * @throws ApiException if the server responds with an HTTP status of 400 or higher
     */
    default @NotNull HypixelForum getSkyBlockPatchNotes() throws ApiException {
        return this.getForumFeed(SKYBLOCK_PATCH_NOTES);
    }

    /**
     * Fetches the RSS feed for the SkyBlock general discussion forum section.
     *
     * @return the decoded forum feed
     * @throws ApiException if the server responds with an HTTP status of 400 or higher
     */
    default @NotNull HypixelForum getSkyBlockGeneralDiscussion() throws ApiException {
        return this.getForumFeed(SKYBLOCK_GENERAL_DISCUSSION);
    }

    /**
     * Fetches the RSS feed for the SkyBlock Alpha top-level section, which is served
     * without a {@code /forums/} prefix.
     *
     * @return the decoded forum feed
     * @throws ApiException if the server responds with an HTTP status of 400 or higher
     */
    default @NotNull HypixelForum getSkyBlockAlpha() throws ApiException {
        return this.getRootFeed(SKYBLOCK_ALPHA);
    }

    /**
     * Fetches the RSS feed for the network-wide news and announcements forum section.
     *
     * @return the decoded forum feed
     * @throws ApiException if the server responds with an HTTP status of 400 or higher
     */
    default @NotNull HypixelForum getNewsAndAnnouncements() throws ApiException {
        return this.getForumFeed(NEWS_AND_ANNOUNCEMENTS);
    }

}
