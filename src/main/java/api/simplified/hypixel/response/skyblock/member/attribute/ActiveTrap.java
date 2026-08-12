package api.simplified.hypixel.response.skyblock.member.attribute;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Region;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * A trap a member has placed somewhere in the world to capture an attribute shard.
 *
 * <p>
 * It records what was placed, where and when, which shard it is set for, and whether it has already
 * fired.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Attributes">Attributes</a>
 */
@Getter
public class ActiveTrap {

    /**
     * The trap instance's own unique id.
     */
    @SerializedName("uuid")
    private UUID uniqueId;

    /**
     * Item id of the trap that was placed.
     */
    @SerializedName("trap_item")
    private String itemId;

    /**
     * When the trap caught something.
     */
    @SerializedName("capture_time")
    private Instant captureTime;

    /**
     * When the trap was set.
     */
    @SerializedName("placed_at")
    private Instant placedAt;

    /**
     * The island game mode the trap sits on. The wire calls this {@code mode}, and it is the value
     * matched against a region's own mode.
     */
    @SerializedName("mode")
    private String remoteId;

    /**
     * Id of the shard the trap is set to capture.
     */
    @SerializedName("shard")
    private String shardId;

    /**
     * Where on that island the trap sits.
     */
    private String location;

    /**
     * Whether the trap has fired.
     */
    private boolean captured;

    /**
     * Whether the trap came out of the hunting toolkit.
     */
    @SerializedName("hunting_toolkit")
    private boolean huntingToolkit;

    /**
     * Which hunting toolkit slot the trap came from.
     */
    @SerializedName("hunting_toolkit_index")
    private int huntingToolkitIndex;

    /**
     * The island the trap sits on, resolved by matching the wire's {@code mode} against the
     * {@link Region} repository.
     *
     * <p>
     * The lookup needs a session, and it hands back null for a mode no region matches rather than
     * failing, so the declared return type is not a guarantee.
     */
    public @NotNull Region getRegion() {
        return SkyBlockData.getRepository(Region.class)
            .findFirstOrNull(Region::getMode, this.getRemoteId());
    }
}
