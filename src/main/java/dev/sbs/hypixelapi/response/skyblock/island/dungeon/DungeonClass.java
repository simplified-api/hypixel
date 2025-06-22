package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.dungeon;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class DungeonClass {

    private final @NotNull Type type;
    private final double experience;

    public enum Type {

        UNKNOWN,
        HEALER,
        MAGE,
        BERSERK,
        ARCHER,
        TANK;

        /*public @NotNull DungeonClassModel getModel() {
            if (this == UNKNOWN)
                throw new UnsupportedOperationException("Unknown does not exist in the database!");

            return SimplifiedApi.getRepositoryOf(DungeonClassModel.class).findFirstOrNull(DungeonClassModel::getKey, this.name());
        }*/

        public static @NotNull Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

}
