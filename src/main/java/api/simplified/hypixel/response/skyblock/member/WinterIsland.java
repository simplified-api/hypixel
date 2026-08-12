package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

/**
 * A member's state on Jerry's Workshop, the winter island.
 *
 * <p>
 * The workshop opens for Late Winter and again for December. The winter event bests - snowball and
 * cannonball hits - are kept on the member's statistics rather than here.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Jerry's_Workshop">Jerry's Workshop</a>
 */
@Getter
public class WinterIsland {

    /**
     * Refined Jyrre consumed on the island.
     */
    @SerializedName("refined_jyrre_uses")
    private int refinedJyrreUses;

}
