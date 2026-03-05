package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface TrophyFish extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull ChatFormat getFormat();

    @NotNull Optional<String> getZoneId();

    enum Tier {

        BRONZE,
        SILVER,
        GOLD,
        DIAMOND

    }

}
