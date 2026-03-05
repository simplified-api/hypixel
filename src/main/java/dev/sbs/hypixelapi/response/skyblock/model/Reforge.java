package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.common.Rarity;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Reforge extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull Optional<String> getStoneId();

    default @NotNull Optional<Item> getStone() {
        return this.getStoneId().flatMap(itemId -> MinecraftApi.getRepository(Item.class)
            .findFirst(Item::getId, itemId)
        );
    }

    default boolean hasStone() {
        return this.getStoneId().isPresent();
    }

    default boolean notStone() {
        return !this.hasStone();
    }

    int getRequiredLevel();

    @NotNull ConcurrentList<String> getCategoryIds();

    default @NotNull ConcurrentList<ItemCategory> getCategories() {
        return MinecraftApi.getRepository(ItemCategory.class)
            .matchAll(category -> this.getCategoryIds().contains(category.getId()))
            .collect(Concurrent.toUnmodifiableList());
    }

    @NotNull ConcurrentList<String> getItemIds();

    default @NotNull ConcurrentList<Item> getItems() {
        return MinecraftApi.getRepository(Item.class)
            .matchAll(item -> this.getItemIds().contains(item.getId()))
            .collect(Concurrent.toUnmodifiableList());
    }

    @NotNull ConcurrentList<Substitute> getStats();

    default @NotNull ConcurrentList<Substitute> getStats(@NotNull Rarity rarity) {
        return this.getStats()
            .stream()
            .filter(stat -> stat.getValues().containsKey(rarity))
            .collect(Concurrent.toUnmodifiableList());
    }

    interface Substitute {

        @NotNull String getId();

        default @NotNull Optional<Stat> getStat() {
            return MinecraftApi.getRepository(Stat.class)
                .findFirst(Stat::getId, this.getId());
        }

        @NotNull ConcurrentMap<Rarity, Double> getValues();

    }

}
