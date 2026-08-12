package api.simplified.hypixel.response.skyblock.member.attribute;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.time.Instant;

/**
 * One stack of one attribute shard in a member's Hunting Box, with the time it was last caught.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Attributes">Attributes</a>
 */
@Getter
public class AttributeShard {

    /**
     * The shard's id, spelled uppercase. It binds as a plain string, so a shard nothing recognises
     * still arrives rather than falling back.
     */
    private String type;

    /**
     * How many of the shard the member holds.
     */
    @SerializedName("amount_owned")
    private int amount;

    /**
     * When one of these shards was last caught, read from epoch milliseconds as an {@link Instant}
     * rather than as the SkyBlock date the rest of the member tree uses.
     */
    private Instant captured;

}
