package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * A potion effect a member is currently under.
 *
 * <p>
 * Potions are brewed consumables that grant timed buffs, and the game tracks the effect, its level
 * and the ticks left before it ends.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Potions">Potions</a>
 */
@Getter
public class PotionData {

    /**
     * Id of the effect. It carries no default, so it is null until the wire supplies one.
     */
    private String effect;

    /**
     * The effect's level.
     */
    private int level;

    /**
     * Ticks left before the effect ends.
     */
    @SerializedName("ticks_remaining")
    private int remainingTicks;

    /**
     * Whether the effect never expires.
     */
    private boolean infinite;

    /**
     * Modifiers applied on top of this effect.
     */
    private @NotNull ConcurrentList<Modifier> modifiers = Concurrent.newList();

    /**
     * One modifier applied on top of a potion effect.
     */
    @Getter
    public static class Modifier {

        /**
         * Id of the modifier. It carries no default, so it is null until the wire supplies one.
         */
        private String key;

        /**
         * How much the modifier amplifies the effect.
         */
        @SerializedName("amp")
        private int amplifier;

    }

}
