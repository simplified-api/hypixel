package api.simplified.hypixel.response.skyblock.member;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * A century cake buff still running on a member.
 *
 * <p>
 * Century cakes are placeable furniture that grant a stat bonus for a long window once eaten, one
 * stat per cake. A member carries one entry per cake still in date, and the buff simply stops
 * counting once {@link #getExpiresAt()} passes.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Century_Cakes">Century Cakes</a>
 */
@Getter
public class CenturyCake {

    @Getter(AccessLevel.NONE)
    @SerializedName("stat_id")
    private @NotNull String statId = "";

    /**
     * Id of the cake itself, which the wire spells as the buffed stat under a {@code cake_} prefix.
     */
    private @NotNull String key = "";

    /**
     * How much of the stat the cake grants.
     */
    private int amount;

    /**
     * When the buff runs out, null on an entry the wire gives no expiry for.
     */
    @SerializedName("expire_at")
    private SkyBlockDate.RealTime expiresAt;

    /**
     * Id of the stat the cake buffs, upper-cased to match the ids the reference data is keyed by -
     * the wire spells it lower-case here, as it does everywhere it names a stat.
     */
    public @NotNull String getStatId() {
        return this.statId.toUpperCase(Locale.ROOT);
    }

    /**
     * Whether the buff is still running, which is false once its window has closed and for an entry
     * carrying no expiry at all.
     */
    public boolean isActive() {
        return this.expiresAt != null && this.expiresAt.getRealTime() > System.currentTimeMillis();
    }

}
