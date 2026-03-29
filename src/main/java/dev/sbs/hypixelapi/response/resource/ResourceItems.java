package dev.sbs.minecraftapi.client.hypixel.response.resource;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.persistence.model.Item;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Information regarding Items.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceItems {

    private boolean success;
    private long lastUpdated;
    private @NotNull ConcurrentList<Item> items = Concurrent.newList();

}
