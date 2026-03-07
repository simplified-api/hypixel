package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.Getter;

@Getter
public class Matriarch {

    @SerializedName("pearls_collected")
    private int lastCollectedPearls;
    @SerializedName("last_attempt")
    private SkyBlockDate.RealTime lastAttempt;
    @SerializedName("recent_refreshes")
    private ConcurrentList<SkyBlockDate.RealTime> recentRefreshes = Concurrent.newList();

}
