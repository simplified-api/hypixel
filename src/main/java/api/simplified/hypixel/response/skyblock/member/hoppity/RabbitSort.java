package api.simplified.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;

/**
 * The rabbit sort order a member last selected in the Hoppity's Collection menu.
 *
 * <p>
 * A saved menu preference rather than progression, ordering the rabbit list alphabetically either
 * way or by rabbit rarity either way. {@link ChocolateFactory} defaults its field to
 * {@link #A_TO_Z}, which is also the in-game default.
 *
 * <p>
 * No constant here is a fallback, so a sort spelling Hypixel adds later binds {@code null} straight
 * over that non-null default.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Chocolate_Rabbits">Chocolate Rabbits</a>
 */
public enum RabbitSort {

    /**
     * Alphabetical, ascending.
     */
    @SerializedName("a_to_z")
    A_TO_Z,

    /**
     * Alphabetical, descending.
     */
    @SerializedName("z_to_a")
    Z_TO_A,

    /**
     * Rarest rabbits first. The wire sends {@code rarity_high_low}; {@code highest_rarity} is kept
     * as an alternate spelling rather than dropped.
     */
    @SerializedName(value = "rarity_high_low", alternate = "highest_rarity")
    HIGHEST_RARITY,

    /**
     * Most common rabbits first. The observed spelling is {@code lowest_rarity} and
     * {@code rarity_low_high} is an unobserved mirror of the spelling the opposite order arrives
     * under, so the two constants are deliberately asymmetric.
     */
    @SerializedName(value = "lowest_rarity", alternate = "rarity_low_high")
    LOWEST_RARITY

}
