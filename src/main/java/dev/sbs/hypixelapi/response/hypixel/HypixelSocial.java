package dev.sbs.minecraftapi.client.hypixel.response.hypixel;

import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
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
