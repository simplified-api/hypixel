package dev.sbs.minecraftapi.client.hypixel.response.skyblock.resource;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceItemsResponse {

    private boolean success;
    private long lastUpdated;
    private @NotNull ConcurrentList<JsonItem> items = Concurrent.newList();

}
