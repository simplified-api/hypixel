package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.Capture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
public class Dojo {

    @Capture(filter = "^dojo_points_")
    private @NotNull ConcurrentMap<Type, Integer> points = Concurrent.newMap();

    @Getter
    @RequiredArgsConstructor
    public enum Type {

        UNKNOWN(""),
        FORCE("mob_kb"),
        STAMINA("wall_jump"),
        MASTERY("archer"),
        DISCIPLINE("sword_swap"),
        SWIFTNESS("snake"),
        CONTROL("fireball"),
        TENACITY("lock_head");

        private final @NotNull String internalName;

        public static @NotNull Dojo.Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name) || type.getInternalName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

}
