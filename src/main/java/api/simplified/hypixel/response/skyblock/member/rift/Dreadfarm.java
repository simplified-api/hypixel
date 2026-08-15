package api.simplified.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * A member's progress on the Dreadfarm, the Rift's version of the Farm.
 *
 * <p>
 * Shania hands over the Wand of Farming and explains its three crops - wilted, agaricus and
 * caducous. Only her dialogue step and the times the Caducous Feeder was used are kept here; the
 * farm's harvest counters are member statistics rather than location state. The field name already
 * matches the wire key, so this node carries no rename.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Dreadfarm">Dreadfarm</a>
 */
@Getter
public class Dreadfarm {

    /**
     * How far Shania's chain has run, bound from {@code shania_stage}.
     */
    @SerializedName("shania_stage")
    private int shaniaStage;

    /**
     * One entry per use of the Caducous Feeder, bound from {@code caducous_feeder_uses}. The wire
     * sends a bare array of epoch milliseconds with no wrapper object and no named timestamp field,
     * and nothing here sorts the result.
     */
    @SerializedName("caducous_feeder_uses")
    private @NotNull ConcurrentList<Instant> caducousFeederUses = Concurrent.newList();

}
