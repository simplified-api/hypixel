package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island;

import dev.sbs.api.collection.concurrent.linked.ConcurrentLinkedMap;
import dev.sbs.minecraftapi.data.model.collection_data.collection_items.CollectionItemModel;
import dev.sbs.minecraftapi.data.model.collection_data.collections.CollectionModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class Collection {

    private final @NotNull CollectionModel type;
    private final @NotNull ConcurrentLinkedMap<CollectionItemModel, Long> collected;
    private final @NotNull ConcurrentLinkedMap<CollectionItemModel, Integer> unlocked;

    public long getCollected(@NotNull CollectionItemModel collectionItemModel) {
        return this.collected.getOrDefault(collectionItemModel, 0L);
    }

    public int getUnlocked(@NotNull CollectionItemModel collectionItemModel) {
        return this.unlocked.getOrDefault(collectionItemModel, 0);
    }

}
