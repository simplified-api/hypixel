package dev.sbs.minecraftapi.client.hypixel.response;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.minecraftapi.data.model.rarities.RarityModel;
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

    /**
     * Gets the {@link RarityModel} for the given {@link Rarity}.
     * <br><br>
     * Requires an active database session.
     */
    public @NotNull RarityModel getModel() {
        if (this == UNKNOWN)
            throw new UnsupportedOperationException("Unknown does not exist in the database!");

        return SimplifiedApi.getRepositoryOf(RarityModel.class).findFirstOrNull(RarityModel::getKey, this.name());
    }

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
