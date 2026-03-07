package dev.sbs.minecraftapi.client.hypixel.response.hypixel;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class HypixelGame {

    private String databaseName;
    private String name;
    private int id;
    private @NotNull ConcurrentMap<String, String> modelNames = Concurrent.newMap();

}
