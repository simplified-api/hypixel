package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

/**
 * A member's fairy soul collection and exchange progress.
 *
 * <p>
 * Fairy souls are collectibles hidden across the world; handing them to Tia the Fairy exchanges them
 * for permanent stat bonuses and storage backpack slots.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Fairy_Souls">Fairy Souls</a>
 */
@Getter
public class FairySouls {

    /**
     * Fairy souls found so far.
     */
    @SerializedName("total_collected")
    private int totalCollected;

    /**
     * Exchanges completed at Tia the Fairy.
     */
    @SerializedName("fairy_exchanges")
    private int exchanges;

    /**
     * Fairy souls found but not yet handed in. The exchange cost changes as the exchanges go up, so
     * this is read from the wire rather than derived from the other two counters.
     */
    @SerializedName("unspent_souls")
    private int unspent;

}
