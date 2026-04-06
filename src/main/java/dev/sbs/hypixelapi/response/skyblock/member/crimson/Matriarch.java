package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
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
