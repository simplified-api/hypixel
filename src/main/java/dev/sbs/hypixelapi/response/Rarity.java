package dev.sbs.minecraftapi.client.hypixel.response;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public enum Rarity {

    UNKNOWN,
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    MYTHIC,
    DIVINE,
    SPECIAL,
    VERY_SPECIAL;

    public static @NotNull Rarity of(@NotNull String name) {
        return Arrays.stream(values())
            .filter(rarity -> rarity.name().equalsIgnoreCase(name))
            .findFirst()
            .orElse(UNKNOWN);
    }

    public static @NotNull Rarity of(int ordinal) {
        return Arrays.stream(values())
            .filter(rarity -> rarity.ordinal() == ordinal)
            .findFirst()
            .orElse(UNKNOWN);
    }

}
