package dev.sbs.minecraftapi.client.hypixel.response.hypixel.implementation;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class HypixelSocial {

    private boolean prompt;
    @Getter private @NotNull ConcurrentMap<Type, String> links = Concurrent.newMap();

    public enum Type {

        TWITTER,
        YOUTUBE,
        INSTAGRAM,
        TWITCH,
        DISCORD,
        HYPIXEL

    }

}
