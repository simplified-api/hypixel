package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Flatten;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class Currencies {

    @SerializedName("motes_purse")
    private int motes;
    @SerializedName("coin_purse")
    private double purse;
    @Flatten("current")
    private @NotNull ConcurrentMap<String, Integer> essence = Concurrent.newMap();

}