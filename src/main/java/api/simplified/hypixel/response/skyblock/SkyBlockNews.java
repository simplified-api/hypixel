package api.simplified.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * The recent news and announcements written for SkyBlock.
 * <p>
 * This is the in-game announcements board rather than the forum at large, so patch notes and
 * network-wide announcements do not appear here.
 */
@Getter
public class SkyBlockNews {

    /**
     * Whether the request was served.
     */
    private boolean success;

    /**
     * The entries on the board, newest first.
     */
    @SerializedName("items")
    private @NotNull ConcurrentList<SkyBlockArticle> articles = Concurrent.newList();

}
