package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.common.Rarity;
import dev.sbs.minecraftapi.client.mojang.profile.MojangProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Accessory extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull Optional<String> getDescription();

    @NotNull Rarity getRarity();

    @NotNull Source getSource();

    @NotNull Limit getLimit();

    @NotNull Optional<MojangProperty> getSkin();

    @NotNull Item.Attributes getAttributes();

    @NotNull Optional<Family> getFamily();

    @NotNull ConcurrentList<Substitute> getStats();

    default @NotNull Item getItem() {
        return MinecraftApi.getRepository(Item.class)
            .findFirstOrNull(Item::getId, this.getId());
    }

    interface Family {

        @NotNull String getId();

        int getRank();

    }

    enum Limit {

        NONE,
        BINGO,
        RIFT

    }

    enum Source {

        COLLECTION,
        NPC,
        EVENT,
        MOB_DROP,
        SLAYER,
        QUEST,
        DUNGEON,
        MISCELLANEOUS,
        RIFT

    }

    interface Substitute {

        @NotNull String getId();

        double getValue();

        default @NotNull Stat getStat() {
            return MinecraftApi.getRepository(Stat.class)
                .findFirstOrNull(Stat::getId, this.getId());
        }

    }

}
