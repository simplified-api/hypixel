package api.simplified.hypixel.response.skyblock.member;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

/**
 * A century cake buff still running on a member.
 *
 * <p>
 * Century cakes are placeable furniture that grant a stat bonus for a long window once eaten, one
 * stat per cake.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Century_Cakes">Century Cakes</a>
 */
@Getter
public class CenturyCake {

    /**
     * Index of the buffed stat in the in-game stat menu's ordering rather than a stat id, and
     * nothing in this module resolves it to one.
     */
    private int stat; // This is in ordinal order in stat menu

    /**
     * The cake's own id.
     */
    private String key;

    /**
     * How much of the stat the cake grants.
     */
    private int amount;

    /**
     * When the buff runs out.
     */
    @SerializedName("expire_at")
    private SkyBlockDate.RealTime expiresAt;

}
