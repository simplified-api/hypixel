package api.simplified.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AllArgsConstructor;
import dev.simplified.annotations.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * The seven rabbits a member hires to produce chocolate automatically.
 *
 * <p>
 * Each employee generates chocolate every second and is levelled up with chocolate. The next one
 * unlocks when the one before it reaches level 20, and its NPC counterpart then appears in the Hub,
 * so declaration order is unlock order - and the chocolate each adds per level rises along it. The
 * factory level sets the employee level cap, which every divine rabbit in the collection raises by
 * one, so a levelled member can sit above the cap the factory level alone implies.
 *
 * <p>
 * Used as a map key on {@link ChocolateFactory}, where an employee id no constant names binds a
 * {@code null} key.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Chocolate_Factory">Chocolate Factory</a>
 */
@Getter
@AllArgsConstructor
public enum RabbitEmployee {

    /**
     * The first employee, available from the start and adding one chocolate per second per level.
     */
    @SerializedName("rabbit_bro")
    BRO("Bro"),

    /**
     * Unlocked at Bro level 20, adding two chocolate per second per level.
     */
    @SerializedName("rabbit_cousin")
    COUSIN("Cousin"),

    /**
     * Unlocked at Cousin level 20, adding three chocolate per second per level.
     */
    @SerializedName("rabbit_sis")
    SISTER("Sis"),

    /**
     * Unlocked at Sis level 20, adding four chocolate per second per level.
     */
    @SerializedName("rabbit_father")
    FATHER("Daddy"),

    /**
     * Unlocked at Daddy level 20, adding five chocolate per second per level.
     */
    @SerializedName("rabbit_grandma")
    GRANDMA("Granny"),

    /**
     * Unlocked at Granny level 20, adding six chocolate per second per level.
     */
    @SerializedName("rabbit_uncle")
    UNCLE("Uncle"),

    /**
     * The last employee, unlocked at Uncle level 20 and adding seven chocolate per second per level.
     */
    @SerializedName("rabbit_dog")
    DOG("Dog");

    /**
     * The employee's in-game label and the name a display should print, which for three of the seven
     * is neither the constant name nor the wire key.
     */
    private final @NotNull String name;

}
