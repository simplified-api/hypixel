package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

public interface Mayor extends Model {

    @NotNull String getId();

    @NotNull String getName();

    boolean isSpecial();

    @NotNull ConcurrentList<Perk> getPerks();

    interface Perk {

        @NotNull String getId();

        @NotNull String getName();

        @NotNull String getDescription();

        @NotNull ConcurrentList<Substitute> getStats();

    }

    interface Substitute {

        @NotNull String getId();

        int getPrecision();

        @NotNull Stat.Type getType();

        @NotNull ChatFormat getFormat();

        double getValue();

    }

}
