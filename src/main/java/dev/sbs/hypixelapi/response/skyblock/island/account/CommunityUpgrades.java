package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.account;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CommunityUpgrades {

    @SerializedName("currently_upgrading")
    private @NotNull Optional<Upgrading> currentlyUpgrading = Optional.empty();
    @SerializedName("upgrade_states")
    private @NotNull ConcurrentList<Upgraded> upgraded = Concurrent.newList();

    @Getter
    public static class Upgraded {

        @SerializedName("upgrade")
        private Type upgrade;
        private int tier;
        @SerializedName("started_ms")
        private SkyBlockDate.RealTime started;
        @SerializedName("started_by")
        private String startedBy;
        @SerializedName("claimed_ms")
        private SkyBlockDate.RealTime claimed;
        @SerializedName("claimed_by")
        private String claimedBy;
        @SerializedName("fasttracked")
        private boolean fastTracked;

    }

    @Getter
    public static class Upgrading {

        @SerializedName("upgrade")
        private Type upgrade;
        @SerializedName("new_tier")
        private int newTier;
        @SerializedName("start_ms")
        private SkyBlockDate.RealTime started;
        @SerializedName("who_started")
        private String startedBy;

    }

    public enum Type {

        @SerializedName("minion_slots")
        MINION_SLOTS,
        @SerializedName("coins_allowance")
        COINS_ALLOWANCE,
        @SerializedName("guests_count")
        GUESTS_COUNT,
        @SerializedName("island_size")
        ISLAND_SIZE,
        @SerializedName("coop_slots")
        COOP_SLOTS

    }

}
