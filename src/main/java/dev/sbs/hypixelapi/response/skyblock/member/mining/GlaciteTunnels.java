package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
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
