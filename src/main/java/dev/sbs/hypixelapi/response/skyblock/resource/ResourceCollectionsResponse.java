package dev.sbs.minecraftapi.client.hypixel.response.skyblock.resource;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.reflection.Reflection;
import dev.sbs.minecraftapi.skyblock.data.json.JsonCollection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.NONE)
public class ResourceCollectionsResponse implements PostInit {

    private static final @NotNull Reflection<JsonCollection> REFLECTION = Reflection.of(JsonCollection.class);
    private boolean success;
    private long lastUpdated;
    private String version;
    @SerializedName("collections")
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, JsonCollection> collectionMap = Concurrent.newMap();
    private transient ConcurrentList<JsonCollection> collections = Concurrent.newList();

    @Override
    public void postInit() {
        this.collections = this.collectionMap.stream()
            .map((id, collection) -> {
                REFLECTION.setValue("id", collection, id.toUpperCase());
                return collection;
            })
            .collect(Concurrent.toUnmodifiableList());
    }

}
