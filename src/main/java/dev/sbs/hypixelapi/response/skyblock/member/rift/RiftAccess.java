package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Getter
public class RiftAccess {

    @SerializedName("last_free")
    private SkyBlockDate.RealTime lastFree;
    @SerializedName("charge_track_timestamp")
    private SkyBlockDate.RealTime chargeTrack;
    @Accessors(fluent = true)
    @SerializedName("consumed_prism")
    private boolean hasConsumedPrism;
    private @NotNull Pass pass = new Pass();

    @Getter
    public static class Pass {

        @SerializedName("issued_at")
        private SkyBlockDate.RealTime issuedAt;
        @SerializedName("rift_server_joins")
        private int serverJoins;
        @Accessors(fluent = true)
        @SerializedName("used_prism")
        private boolean hasUsedPrism;

    }

}
