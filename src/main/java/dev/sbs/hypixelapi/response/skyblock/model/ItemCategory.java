package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.persistence.Model;
import org.jetbrains.annotations.NotNull;

public interface ItemCategory extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull Item.Type getType();

}
