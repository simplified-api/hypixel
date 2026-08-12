package api.simplified.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One powder balance on the Heart of the Mountain.
 *
 * <p>
 * Powder is the currency that buys and upgrades Heart of the Mountain perks. It is earned from
 * commissions, from mining the region's ore and from the daily first-ore bonus, and each of the
 * three mining regions pays in its own kind. This carries the balance available, the lifetime total
 * and the amount already spent.
 *
 * <p>
 * The three fields are <b>affix selectors inside the capturing map that holds them, not literal wire
 * keys</b>. The capture strips the leading {@code powder_} and groups what remains by the affixes
 * declared here, so the mithril balance arrives as the three keys {@code powder_mithril},
 * {@code powder_mithril_total} and {@code powder_spent_mithril}. The no-argument constructor is
 * public so the capture can instantiate a group.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Heart_of_the_Mountain">Heart of the Mountain</a>
 */
@Getter
@NoArgsConstructor
public class Powder {

    /**
     * The powder currently available to spend.
     *
     * <p>
     * The empty name is deliberate and must not be filled in: it selects the bare captured key, the
     * one with no affix on either end.
     */
    @SerializedName("")
    private int amount;

    /**
     * The lifetime total the wire reports for this powder.
     *
     * <p>
     * Bound from the {@code _total} suffix. It is neither the balance plus the amount spent nor
     * reliably positive - Hypixel's own bookkeeping drifts - so it is not a derivable value and not
     * a maximum.
     */
    private int total;

    /**
     * Powder already sunk into perks.
     *
     * <p>
     * {@code spent_} is a prefix on the stripped key rather than a suffix, so the wire spells this
     * {@code powder_spent_mithril} while the total is {@code powder_mithril_total}. The two are not
     * symmetric and neither is derivable from the field name.
     */
    @SerializedName("spent_")
    private int spent;

    /**
     * The three kinds of powder, one per mining region.
     *
     * <p>
     * No constant declares a wire name; what is left of a captured key once {@code powder_} and the
     * field affix are stripped matches a constant case-insensitively through the shared lookup.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Heart_of_the_Mountain">Heart of the Mountain</a>
     */
    public enum Type {

        /**
         * Mithril Powder, earned from Dwarven Mines commissions, from mithril ore and from the
         * Dwarven Mines daily first-ore bonus.
         */
        MITHRIL,

        /**
         * Gemstone Powder, earned from Crystal Hollows commissions, from gemstone veins and from the
         * Crystal Hollows daily first-ore bonus.
         */
        GEMSTONE,

        /**
         * Glacite Powder, earned from Glacite Tunnels commissions, from glacite, tungsten and umber
         * ore and from the Glacite Tunnels daily first-ore bonus.
         */
        GLACITE

    }

}
