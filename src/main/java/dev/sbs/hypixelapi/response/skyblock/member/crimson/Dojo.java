package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.tuple.pair.Pair;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
public class Dojo {

    private final @NotNull ConcurrentMap<Type, Integer> points;

    public Dojo() {
        this(Concurrent.newMap());
    }

    public Dojo(@NotNull ConcurrentMap<String, Integer> dojo) {
        this.points = Concurrent.newUnmodifiableMap(
            dojo.stream()
                .filter(entry -> !entry.getKey().contains("time_"))
                .map(entry -> Pair.of(Dojo.Type.of(entry.getKey().replace("dojo_points_", "")), entry.getValue()))
                .collect(Concurrent.toMap())
        );
    }

    public int getPoints(@NotNull Dojo.Type type) {
        return this.getPoints().getOrDefault(type, 0);
    }

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
