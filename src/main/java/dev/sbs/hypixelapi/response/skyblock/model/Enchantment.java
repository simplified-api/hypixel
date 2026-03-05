package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.common.Rarity;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Enchantment extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getDescription();

    @NotNull Type getType();

    int getRequiredLevel();

    @NotNull Optional<String> getConflict();

    default boolean hasConflict() {
        return this.getConflict().isPresent();
    }

    default boolean noConflict() {
        return !this.hasConflict();
    }

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

    @NotNull ConcurrentList<Level> getLevels();

    @NotNull ConcurrentList<Substitute> getStats();

    @NotNull ConcurrentList<String> getMobTypeIds();

    default @NotNull ConcurrentList<MobType> getMobTypes() {
        return MinecraftApi.getRepository(MobType.class)
            .matchAll(mobType -> this.getMobTypeIds().contains(mobType.getId()))
            .collect(Concurrent.toUnmodifiableList());
    }

    interface ApplyCost {

        int getExperience();

        @NotNull Optional<String> getItemId();

        default @NotNull Optional<Item> getItem() {
            return this.getItemId().flatMap(itemId -> MinecraftApi.getRepository(Item.class)
                .findFirst(Item::getId, itemId)
            );
        }

    }

    interface Level {

        int getLevel();

        default @NotNull Rarity getRarity() {
            return switch (this.getLevel()) {
                case 9, 10 -> Rarity.MYTHIC;
                case 8 -> Rarity.LEGENDARY;
                case 7 -> Rarity.EPIC;
                case 6 -> Rarity.RARE;
                case 5 -> Rarity.UNCOMMON;
                default -> Rarity.COMMON;
            };
        }

        @NotNull ApplyCost getApplyCost();

    }

    interface Substitute {

        @NotNull String getId();

        default @NotNull Optional<Stat> getStat() {
            return MinecraftApi.getRepository(Stat.class)
                .findFirst(Stat::getId, this.getId());
        }

        int getPrecision();

        @NotNull Stat.Type getType();

        @NotNull ChatFormat getFormat();

        @NotNull ConcurrentMap<Integer, Double> getValues();

    }

    @Getter
    @RequiredArgsConstructor
    enum Type {

        NORMAL(ChatFormat.BLUE, false),
        ULTIMATE(ChatFormat.LIGHT_PURPLE, true);

        private final @NotNull ChatFormat format;
        private final boolean bold;

        @Override
        public String toString() {
            return this.getFormat() + (this.isBold() ? ChatFormat.BOLD.toString() : "");
        }
    }

}
