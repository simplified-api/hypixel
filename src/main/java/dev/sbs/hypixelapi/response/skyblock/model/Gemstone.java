package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

public interface Gemstone extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getSymbol();

    @NotNull ChatFormat getFormat();

    @NotNull String getStatId();

    default @NotNull Stat getStat() {
        return MinecraftApi.getRepository(Stat.class).findFirstOrNull(Stat::getId, this.getStatId());
    }

    @NotNull ConcurrentMap<Type, ConcurrentMap<ChatFormat, Double>> getValues();

    enum Type {

        @SerializedName("rough")
        ROUGH,
        @SerializedName("flawed")
        FLAWED,
        @SerializedName("fine")
        FINE,
        @SerializedName("flawless")
        FLAWLESS,
        @SerializedName("perfect")
        PERFECT;

    }

}
