package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.skyblock.Article;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class SkyBlockNewsResponse {

    private boolean success;
    @SerializedName("items")
    private @NotNull ConcurrentList<Article> articles = Concurrent.newList();

}
