package dev.sbs.minecraftapi.client.hypixel.response.hypixel;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class HypixelGame {

    private int id;
    @SerializedName("databaseName")
    private @NotNull String databaseName;
    @SerializedName("name")
    private @NotNull String name;
    private boolean legacy;
    private boolean retired;
    private @NotNull ConcurrentMap<String, String> modelNames = Concurrent.newMap();

}
