package dev.sbs.minecraftapi.client.hypixel.response.resource;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class ResourceCollectionsResponse {

    private boolean success;
    private long lastUpdated;
    private String version;
    private @NotNull ConcurrentMap<String, Collection> collections = Concurrent.newMap();

    @Getter
    public static class Collection {

        private String name;
        private @NotNull ConcurrentMap<String, CollectionItem> items = Concurrent.newMap();

    }

    @Getter
    public static class CollectionItem {

        private String name;
        private int maxTiers;
        private @NotNull ConcurrentList<CollectionTier> tiers = Concurrent.newList();

    }

    @Getter
    public static class CollectionTier {

        private int tier;
        private double amountRequired;
        private @NotNull ConcurrentList<String> unlocks = Concurrent.newList();

    }

}
