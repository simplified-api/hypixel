package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.Model;
import org.jetbrains.annotations.NotNull;

public interface Collection extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull ConcurrentMap<String, Item> getItems();

    interface Item {

        @NotNull String getName();

        int getMaxTiers();

        @NotNull ConcurrentList<Tier> getTiers();

        default int getMaxRequired() {
            return this.getTiers().get(this.getTiers().size() - 1).getAmountRequired();
        }

    }

    interface Tier {

        int getTier();

        int getAmountRequired();

        @NotNull ConcurrentList<String> getUnlocks();

    }

}
