package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.api.tuple.pair.PairOptional;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.persistence.model.TrophyFish;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class TrophyFishing {

    private final @NotNull ConcurrentMap<String, ConcurrentMap<TrophyFish.Tier, Integer>> fish;
    private final int totalCaught;
    private final @NotNull PairOptional<String, TrophyFish.Tier> lastCaught;

    public TrophyFishing(@NotNull ConcurrentMap<String, Object> trophy_fish) {
        ConcurrentMap<String, Object> trophy_fishMap = Concurrent.newMap(trophy_fish);
        this.totalCaught = (int) trophy_fishMap.removeOrGet("total_caught", 0);

        this.lastCaught = PairOptional.of(
            trophy_fishMap.getOptional("last_caught")
                .map(String::valueOf)
                .map(value -> value.split("/"))
                .map(parts -> Pair.of(parts[0], TrophyFish.Tier.valueOf(parts[1])))
        );

        this.fish = MinecraftApi.getRepository(TrophyFish.class)
            .stream()
            .map(type -> Pair.of(
                type.getId(),
                trophy_fishMap.stream()
                    .filter(entry -> entry.getKey().startsWith(type.getId().toLowerCase()))
                    .map(entry -> Pair.of(
                        TrophyFish.Tier.valueOf(entry.getKey().replace(type.getId(), "")),
                        (int) entry.getValue()
                    ))
                    .collect(Concurrent.toMap())
            ))
            .collect(Concurrent.toMap());
    }

}
