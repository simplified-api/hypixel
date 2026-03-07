package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class GlaciteTunnels {

    @SerializedName("fossils_donated")
    private @NotNull ConcurrentList<String> donatedFossils = Concurrent.newList();
    @SerializedName("fossil_dust")
    private double fossilDust;
    @SerializedName("corpses_looted")
    private @NotNull ConcurrentMap<CorpseType, Integer> lootedCorpses = Concurrent.newMap();
    @SerializedName("mineshafts_entered")
    private int enteredMineshafts;

    public enum CorpseType {

        LAPIS,
        TUNGSTEN,
        UMBER,
        VANGUARD

    }

}
