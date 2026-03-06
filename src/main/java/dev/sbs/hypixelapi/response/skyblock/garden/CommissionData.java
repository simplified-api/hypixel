package dev.sbs.minecraftapi.client.hypixel.response.skyblock.garden;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor
public class CommissionData {

    private @NotNull ConcurrentMap<String, Integer> visits = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Integer> completed = Concurrent.newMap();
    private int totalCompleted;
    @SerializedName("unique_npcs_served")
    private int uniqueNPCsServed;

}
