package api.simplified.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;

/**
 * The rabbit filter a member last selected in the Hoppity's Collection menu.
 *
 * <p>
 * A saved menu preference rather than progression: it narrows the rabbit list by whether a rabbit
 * has been found and by whether it is gated behind an unlock requirement. The wire omits it for a
 * member who never opened the menu, which is why {@link ChocolateFactory} defaults its field to
 * {@link #NONE}.
 *
 * <p>
 * No constant here is a fallback, so a filter spelling Hypixel adds later binds {@code null}
 * straight over that non-null default.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Chocolate_Rabbits">Chocolate Rabbits</a>
 */
public enum RabbitFilter {

    /**
     * No filter, showing every rabbit. Carries no wire spelling of its own and binds off the
     * constant name, which works only because lookup is case-insensitive.
     */
    NONE,

    /**
     * Only rabbits already collected.
     */
    @SerializedName("found")
    FOUND,

    /**
     * Only rabbits still missing.
     */
    @SerializedName("not_found")
    NOT_FOUND,

    /**
     * Only rabbits gated behind an unlock requirement, which the wire spells {@code achievements}
     * even though the gate is a requirement on the rabbit rather than a Hypixel achievement.
     */
    @SerializedName("achievements")
    HAS_REQUIREMENT,

    /**
     * Only rabbits with no unlock requirement, which the wire spells {@code non_achievements}.
     */
    @SerializedName("non_achievements")
    NO_REQUIREMENT

}
