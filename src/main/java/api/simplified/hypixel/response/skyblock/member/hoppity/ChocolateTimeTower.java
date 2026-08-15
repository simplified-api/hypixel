package api.simplified.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;

import java.time.Instant;

/**
 * The Time Tower upgrade on a member's Chocolate Factory, unlocked at factory level 2.
 *
 * <p>
 * The tower banks one charge every eight real hours - seven with the Einstein rabbit - and each
 * charge is activated on its own to apply a temporary chocolate-per-second multiplier for one hour,
 * from {@code +0.1x} at the first level up to {@code +1.5x} at level 15. The permanently active
 * multiplier is Coach Jackrabbit on {@link ChocolateFactory}, not this one.
 *
 * <p>
 * A member who has never activated a charge sends only the last charge time, so both instants here
 * are nullable in practice.
 */
@Getter
public class ChocolateTimeTower {

    /**
     * Unspent charges banked and ready to activate.
     */
    private int charges;

    /**
     * When the running charge was activated; its multiplier holds for an hour from that point, and
     * the value is null until a charge has been activated.
     */
    @SerializedName("activation_time")
    private Instant activationTime;

    /**
     * Tower level, capped at 15, which sets how large the temporary multiplier is.
     */
    private int level;

    /**
     * When the tower last finished accruing a charge.
     */
    @SerializedName("last_charge_time")
    private Instant lastChargeTime;

}
