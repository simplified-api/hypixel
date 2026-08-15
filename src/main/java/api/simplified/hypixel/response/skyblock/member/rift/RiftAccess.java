package api.simplified.hypixel.response.skyblock.member.rift;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NamingStyle;
import org.jetbrains.annotations.NotNull;

/**
 * How a member gets into the Rift, and how much entry credit is left.
 *
 * <p>
 * Entry costs a Dimensional Infusion - one is granted free every four hours up to three stored, and
 * it is spent on leaving rather than on entering, lasting three uses. Holding a Rift Prism is an
 * alternative way in, which is why consuming one is tracked as a permanent flag.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Rift_Dimension">Rift Dimension</a>
 */
@Getter
public class RiftAccess {

    /**
     * When the last free Dimensional Infusion was granted, bound from {@code last_free}. Null on a
     * member who never entered, and {@link SkyBlockDate.RealTime} binds it from raw epoch
     * milliseconds.
     */
    @SerializedName("last_free")
    private SkyBlockDate.RealTime lastFree;

    /**
     * The anchor the four-hourly infusion charge accrual is measured from, bound from
     * {@code charge_track_timestamp}. A separate instant from the last free grant and, like it,
     * null on a member who never entered.
     */
    @SerializedName("charge_track_timestamp")
    private SkyBlockDate.RealTime chargeTrack;

    /**
     * Whether a Rift Prism has been consumed, bound from {@code consumed_prism}. This is lifetime
     * state on the member and is not the same fact as the per-pass {@code used_prism}.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("consumed_prism")
    private boolean hasConsumedPrism;

    /**
     * The current entry pass, empty of values until the wire sends a {@code pass} node.
     */
    private @NotNull Pass pass = new Pass();

    /**
     * One entry pass into the Rift.
     */
    @Getter
    public static class Pass {

        /**
         * When this pass was issued, bound from {@code issued_at} as raw epoch milliseconds.
         */
        @SerializedName("issued_at")
        private SkyBlockDate.RealTime issuedAt;

        /**
         * How many Rift servers this pass has been carried onto, bound from
         * {@code rift_server_joins}.
         */
        @SerializedName("rift_server_joins")
        private int serverJoins;

        /**
         * Whether this pass was opened with the Rift Prism, bound from {@code used_prism} - a fact
         * about this one pass rather than about the member.
         */
        @Getter(style = NamingStyle.FLUENT)
        @SerializedName("used_prism")
        private boolean hasUsedPrism;

    }

}
