package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;

/**
 * A member's item preferences and the permanent item unlocks that sit beside them.
 *
 * <p>
 * The wire groups three unrelated settings here, so the node is a grouping rather than an in-game
 * concept of its own.
 */
@Getter
public class ItemSettings {

    /**
     * Whether the teleporter pill has been consumed, the permanent unlock for travelling between
     * teleport pads on the private island.
     */
    @SerializedName("teleporter_pill_consumed")
    private boolean teleporterPillConsumed;

    /**
     * The arrow the quiver fires by default. It defaults to {@code ARROW}, so an absent wire key and
     * a wire value of {@code ARROW} are indistinguishable once bound.
     */
    @SerializedName("favorite_arrow")
    private String favoriteArrow = "ARROW";

    /**
     * Internalized soulflow held, the reserve that fuels Enderman Slayer abilities.
     */
    private int soulflow;

}
