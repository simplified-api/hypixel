package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.hoppity;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.util.NumberUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Getter
public class RabbitCollection {

    private final @NotNull ConcurrentMap<String, Long> eggs;
    private final @NotNull ConcurrentMap<String, Integer> rabbits;
    private final @NotNull ConcurrentMap<String, ConcurrentList<String>> locations;

    @SuppressWarnings("unchecked")
    public RabbitCollection(@NotNull ConcurrentMap<String, Object> rabbits) {
        ConcurrentMap<String, Object> rabbitMap = Concurrent.newMap(rabbits);
        this.eggs = Concurrent.newMap((Map<String, Long>) rabbitMap.get("eggs"));
        this.locations = Concurrent.newMap((Map<String, List<String>>) rabbitMap.get("locations"))
            .stream()
            .mapValue(Concurrent::newList)
            .collect(Concurrent.toUnmodifiableMap());
        this.rabbits = rabbitMap.stream()
            .mapValue(String::valueOf)
            .mapValue(NumberUtil::toInt)
            .collect(Concurrent.toUnmodifiableMap());
    }

}
