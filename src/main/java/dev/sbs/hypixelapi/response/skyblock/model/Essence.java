package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

public interface Essence extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull ChatFormat getFormat();

}
