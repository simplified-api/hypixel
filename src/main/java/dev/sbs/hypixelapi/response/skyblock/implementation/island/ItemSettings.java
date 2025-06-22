package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class ItemSettings {

    @SerializedName("teleporter_pill_consumed")
    private boolean teleporterPillConsumed;
    @SerializedName("favorite_arrow")
    private String favoriteArrow;
    private int soulflow;

}
