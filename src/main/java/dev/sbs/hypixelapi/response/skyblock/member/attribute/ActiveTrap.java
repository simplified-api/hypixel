package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.attribute;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.skyblock.model.Region;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ActiveTrap {

    @SerializedName("uuid")
    private UUID uniqueId;
    @SerializedName("trap_item")
    private String itemId;
    @SerializedName("capture_time")
    private Instant captureTime;
    @SerializedName("placed_at")
    private Instant placedAt;
    @SerializedName("mode")
    private String remoteId;
    @SerializedName("shard")
    private String shardId;
    private String location;
    private boolean captured;
    @SerializedName("hunting_toolkit")
    private boolean huntingToolkit;
    @SerializedName("hunting_toolkit_index")
    private int huntingToolkitIndex;

    public @NotNull Region getRegion() {
        return MinecraftApi.getRepository(Region.class)
            .findFirstOrNull(Region::getMode, this.getRemoteId());
    }
}
