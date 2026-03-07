package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Getter
public class EnigmasCrib {

    @Accessors(fluent = true)
    @SerializedName("bought_cloak")
    private boolean hasBoughtCloak;
    @SerializedName("found_souls")
    private @NotNull ConcurrentList<String> foundSouls = Concurrent.newList();
    @SerializedName("claimed_bonus_index")
    private int claimedBonusIndex;

}
