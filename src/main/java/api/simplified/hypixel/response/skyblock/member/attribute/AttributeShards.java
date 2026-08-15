package api.simplified.hypixel.response.skyblock.member.attribute;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.gson.annotation.SerializedPath;
import org.jetbrains.annotations.NotNull;

/**
 * A member's attribute shards - what sits in the Hunting Box, what has been fused away, and what
 * traps are out in the world.
 *
 * <p>
 * Attributes are passive buffs obtained through Hunting. Each attribute has a matching shard, an
 * item that lands in the Hunting Box when caught; a shard can be fused into another shard or
 * syphoned into its attribute, and each syphon raises that attribute's level up to ten.
 *
 * <p>
 * The distinct-shard count and the per-source hunt counters are kept on the member's statistics
 * rather than here.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Attributes">Attributes</a>
 */
@Getter
public class AttributeShards {

    /**
     * Traps the member currently has placed in the world, read from the nested
     * {@code traps.active_traps} node.
     */
    @SerializedPath("traps.active_traps")
    private @NotNull ConcurrentList<ActiveTrap> activeTraps = Concurrent.newList();

    /**
     * Every shard stacked in the Hunting Box.
     */
    @SerializedName("owned")
    private @NotNull ConcurrentList<AttributeShard> ownedShards = Concurrent.newList();

    /**
     * Shards consumed by fusion.
     */
    private int fused;

}
