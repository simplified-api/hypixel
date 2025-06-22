package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.crimson_isle;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.stream.pair.Pair;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
public class TrophyFish {

    private final @NotNull ConcurrentMap<Type, ConcurrentMap<Tier, Integer>> fish;
    private final int totalCaught;

    public TrophyFish(@NotNull ConcurrentMap<String, Object> trophy_fish) {
        this.totalCaught = (int) trophy_fish.removeOrGet("total_caught", 0);

        this.fish = Arrays.stream(Type.values())
            .filter(type -> type != Type.UNKNOWN)
            .map(type -> Pair.of(
                type,
                trophy_fish.stream()
                    .filter(entry -> entry.getKey().startsWith(type.name().toLowerCase()))
                    .map(entry -> Pair.of(
                        Tier.of(entry.getKey().replace(type.name(), "")),
                        (int) entry.getValue()
                    ))
                    .collect(Concurrent.toMap())
            ))
            .collect(Concurrent.toMap());
    }

    public enum Tier {

        UNKNOWN,
        BRONZE,
        SILVER,
        GOLD,
        DIAMOND;

        public static @NotNull Tier of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

    public enum Type {

        UNKNOWN,
        BLOBFISH,
        GUSHER,
        STEAMING_HOT_FLOUNDER,
        SULPHUR_SKITTER,
        OBFUSCATED_FISH_1,
        FLYFISH,
        SLUGFISH,
        OBFUSCATED_FISH_2,
        LAVA_HORSE,
        MANA_RAY,
        VANILLE,
        VOLCANIC_STONEFISH,
        OBFUSCATED_FISH_3,
        KARATE_FISH,
        MOLDFIN,
        SKELETON_FISH,
        SOUL_FISH,
        GOLDEN_FISH;

        /*public @NotNull TrophyFishModel getModel() {
            return SimplifiedApi.getRepositoryOf(TrophyFishModel.class).findFirstOrNull(TrophyFishModel::getKey, this.name());
        }*/

        public @NotNull Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

}
