package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Recent News and Announcements focused on SkyBlock.
 * <p>
 * This does not include Patch Notes or other announcements.
 */
@Getter
public class SkyBlockNews {

    private boolean success;
    @SerializedName("items")
    private @NotNull ConcurrentList<SkyBlockArticle> articles = Concurrent.newList();

}
