package dev.sbs.minecraftapi.client.hypixel.response.forum;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndCategoryImpl;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.SyndFeedOutput;
import dev.sbs.minecraftapi.client.hypixel.HypixelForumContract;
import dev.simplified.client.codec.XmlDecoder;
import dev.simplified.client.codec.XmlEncoder;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.gson.adapter.Rfc822InstantAdapter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Typed view of a XenForo-generated RSS 2.0 feed served by {@code hypixel.net/forums/*},
 * normalized across every forum section exposed by the site.
 * <p>
 * The class mirrors the top-level {@code <rss>}/{@code <channel>}/{@code <item>}
 * hierarchy produced by XenForo's RSS syndication, including the Dublin Core
 * ({@code dc:}), RSS Content Module ({@code content:}), and Slash ({@code slash:})
 * namespace extensions. Namespace prefixes are stripped by
 * {@link XmlDecoder XmlDecoder} before Gson binding, so each
 * extension element is modeled against its bare local name (for example, {@code dc:creator}
 * binds to {@link Item#getCreator()}).
 *
 * <p><b>Namespace collision workaround.</b> The feed declares both a standard RSS
 * {@code <link>} element and an {@code <atom:link rel="self">} sibling inside
 * {@code <channel>}. Because Jackson's tree mode strips prefixes, both collapse to the
 * same local name and the naive parse produces a mixed-type array for {@code channel.link}.
 * The Hypixel forum {@link XmlDecoder XmlDecoder} installs a tree transformer that folds
 * the array down to the text element before Gson binding, so this DTO simply exposes
 * {@code link} as a {@link String}.
 *
 * <p><b>Round-trip serialization.</b> Writing a {@code HypixelForum} back to valid RSS XML
 * goes through ROME rather than Jackson, because Jackson's write path does not track
 * namespace URIs from the original read and produces output that some strict aggregators
 * reject. The static {@link #toSyndFeed(HypixelForum)} method converts an instance into a
 * ROME {@link SyndFeed}, which {@link XmlEncoder XmlEncoder} hands off to
 * {@link SyndFeedOutput SyndFeedOutput} for proper namespace-aware emission.
 *
 * @see HypixelForumContract
 * @see XmlDecoder
 * @see XmlEncoder
 * @see <a href="https://hypixel.net/forums/news-and-announcements.4/-/index.rss">Example feed</a>
 */
@Getter
@NoArgsConstructor
public class HypixelForum {

    /** The single {@code <channel>} element declared under {@code <rss>}. */
    private @NotNull Channel channel = new Channel();

    /**
     * Converts a {@code HypixelForum} instance into a ROME {@link SyndFeed} suitable for
     * writing back to RSS 2.0 XML via
     * {@link com.rometools.rome.io.SyndFeedOutput SyndFeedOutput}.
     * <p>
     * Channel-level metadata (title, description, link, language, dates, generator) maps
     * onto the corresponding {@link SyndFeed} properties; each {@link Item} becomes a
     * {@link SyndEntry} with the {@code content:encoded} HTML body exposed as a
     * {@code text/html} {@link SyndContent}, the {@code dc:creator} (or plain
     * {@code author}) as the entry author, and the category and guid mirrored onto the
     * ROME analogues.
     *
     * @param forum the forum DTO to convert
     * @return a fully populated {@link SyndFeed} with {@code feedType = "rss_2.0"}
     */
    public static @NotNull SyndFeed toSyndFeed(@NotNull HypixelForum forum) {
        Channel channel = forum.getChannel();
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        feed.setTitle(channel.getTitle());
        feed.setDescription(channel.getDescription());
        feed.setLink(channel.getLink());
        feed.setLanguage(channel.getLanguage());
        feed.setGenerator(channel.getGenerator());

        if (!channel.getPublishedAt().equals(Instant.EPOCH))
            feed.setPublishedDate(Date.from(channel.getPublishedAt()));

        List<SyndEntry> entries = new ArrayList<>(channel.getItems().size());

        for (Item item : channel.getItems()) {
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(item.getTitle());
            entry.setLink(item.getLink());
            entry.setUri(item.getGuid().getValue());
            entry.setAuthor(item.getCreator().isEmpty() ? item.getAuthor() : item.getCreator());

            if (!item.getPublishedAt().equals(Instant.EPOCH))
                entry.setPublishedDate(Date.from(item.getPublishedAt()));

            SyndContent description = new SyndContentImpl();
            description.setType("text/html");
            description.setValue(item.getEncoded());
            entry.setDescription(description);

            SyndCategory category = new SyndCategoryImpl();
            category.setName(item.getCategory().getValue());
            category.setTaxonomyUri(item.getCategory().getDomain());
            entry.setCategories(List.of(category));

            entries.add(entry);
        }

        feed.setEntries(entries);
        return feed;
    }

    /**
     * Top-level {@code <channel>} metadata and the ordered list of feed entries.
     */
    @Getter
    @NoArgsConstructor
    public static class Channel {

        /** Channel language tag, e.g. {@code "en-US"}. */
        private @NotNull String language = "";

        /** Channel title, e.g. {@code "News and Announcements"}. */
        private @NotNull String title = "";

        /** Channel description. */
        private @NotNull String description = "";

        /** Publication timestamp of the feed itself, parsed from RFC 822 {@code <pubDate>}. */
        @SerializedName("pubDate")
        @JsonAdapter(Rfc822InstantAdapter.class)
        private @NotNull Instant publishedAt = Instant.EPOCH;

        /** Timestamp at which the feed was last regenerated, parsed from RFC 822 {@code <lastBuildDate>}. */
        @SerializedName("lastBuildDate")
        @JsonAdapter(Rfc822InstantAdapter.class)
        private @NotNull Instant lastBuiltAt = Instant.EPOCH;

        /** Name of the tool that produced the feed, e.g. {@code "Hypixel Forums"}. */
        private @NotNull String generator = "";

        /** Canonical link to the forum section, resolved from the text-content {@code <link>} element. */
        private @NotNull String link = "";

        /** The ordered list of feed entries, one per {@code <item>} element. */
        @SerializedName("item")
        private @NotNull ConcurrentList<Item> items = Concurrent.newList();

    }

    /**
     * A single entry in the RSS feed, corresponding to one forum thread.
     */
    @Getter
    @NoArgsConstructor
    public static class Item {

        /** Thread title. */
        private @NotNull String title = "";

        /** Publication timestamp of the entry, parsed from RFC 822 {@code <pubDate>}. */
        @SerializedName("pubDate")
        @JsonAdapter(Rfc822InstantAdapter.class)
        private @NotNull Instant publishedAt = Instant.EPOCH;

        /** Canonical link to the thread. */
        private @NotNull String link = "";

        /** Globally unique identifier for the thread, with an {@code isPermaLink} attribute. */
        private @NotNull Guid guid = new Guid();

        /** Plain-text author in the form {@code invalid@example.com (Name)}. */
        private @NotNull String author = "";

        /** The {@code <category>} element, carrying a {@code domain} attribute and CDATA text. */
        private @NotNull Category category = new Category();

        /**
         * The Dublin Core author from {@code dc:creator}, typically a bare display name such
         * as {@code "Hypixel Team"} - preferred over {@link #getAuthor()} for display.
         */
        private @NotNull String creator = "";

        /**
         * The full HTML body of the first post, extracted from the
         * {@code content:encoded} CDATA block.
         */
        private @NotNull String encoded = "";

        /** Comment count for the thread, extracted from {@code slash:comments}. */
        private int comments;

    }

    /**
     * The {@code <guid>} element: a unique identifier string with an {@code isPermaLink}
     * attribute indicating whether the identifier is also a resolvable URL.
     */
    @Getter
    @NoArgsConstructor
    public static class Guid {

        /** Whether the {@link #getValue() guid value} is a resolvable URL. */
        private boolean isPermaLink;

        /** The guid value itself, read from the mixed-content text under the {@value dev.simplified.client.codec.XmlDecoder#TEXT_KEY} key. */
        @SerializedName("$")
        private @NotNull String value = "";

    }

    /**
     * The {@code <category>} element: a taxonomy domain URI plus a human-readable category
     * name held in a CDATA section.
     */
    @Getter
    @NoArgsConstructor
    public static class Category {

        /** The taxonomy URI for the category, e.g. a forum section permalink. */
        private @NotNull String domain = "";

        /** The category name, read from the mixed-content text under the {@value dev.simplified.client.codec.XmlDecoder#TEXT_KEY} key. */
        @SerializedName("$")
        private @NotNull String value = "";

    }

}
