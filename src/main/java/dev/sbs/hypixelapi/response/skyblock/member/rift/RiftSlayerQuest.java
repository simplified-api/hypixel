package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.slayer.SlayerQuest;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class RiftSlayerQuest extends SlayerQuest {

    @SerializedName("combat_xp")
    private int combatXP;
    @SerializedName("recent_mob_kills")
    private @NotNull ConcurrentList<MobKill> recentMobKills = Concurrent.newList();
    @SerializedName("last_killed_mob_island")
    private String lastKilledMobIsland;

    @Getter
    public static class MobKill {

        private int xp;
        private SkyBlockDate.RealTime timestamp;

    }

}
