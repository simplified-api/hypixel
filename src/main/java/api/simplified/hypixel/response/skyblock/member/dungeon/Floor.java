package api.simplified.hypixel.response.skyblock.member.dungeon;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.EnumLookup;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.KeyField;
import dev.simplified.annotations.RequiredArgsConstructor;
import dev.simplified.util.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
 * One of the eight Catacombs floors.
 * <p>
 * The Entrance is the tutorial floor with no final boss - it ends when The Watcher is defeated - and
 * floors 1 through 7 each end on their own boss. Every floor also exists in master mode, with the
 * same boss and harder mobs, except the Entrance, which master mode does not have.
 * <p>
 * Each constant carries a {@link SerializedName} of its wire digit, and that is what makes a
 * floor-keyed map round-trip as the number the wire sent rather than as the constant name.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Catacombs">Catacombs</a>
 */
@Getter
@EnumLookup
@RequiredArgsConstructor
public enum Floor {

    /**
     * The Catacombs - Entrance, the starting floor. It has no final boss, ending on The Watcher
     * instead, and no reward chests spawn here.
     */
    @SerializedName("0")
    ENTRANCE(0, Size.TINY, Boss.WATCHER),

    /**
     * The Catacombs - Floor I, ending on Bonzo.
     */
    @SerializedName("1")
    ONE(1, Size.TINY, Boss.BONZO),

    /**
     * The Catacombs - Floor II, ending on Scarf.
     */
    @SerializedName("2")
    TWO(2, Size.SMALL, Boss.SCARF),

    /**
     * The Catacombs - Floor III, ending on The Professor. Clearing it earns Saul's Recommendation
     * and with it solo mode.
     */
    @SerializedName("3")
    THREE(3, Size.SMALL, Boss.THE_PROFESSOR),

    /**
     * The Catacombs - Floor IV, ending on Thorn.
     */
    @SerializedName("4")
    FOUR(4, Size.SMALL, Boss.THORN),

    /**
     * The Catacombs - Floor V, ending on Livid.
     */
    @SerializedName("5")
    FIVE(5, Size.MEDIUM, Boss.LIVID),

    /**
     * The Catacombs - Floor VI, ending on Sadan. The first floor where the Mimic can spawn and grant
     * bonus score.
     */
    @SerializedName("6")
    SIX(6, Size.MEDIUM, Boss.SADAN),

    /**
     * The Catacombs - Floor VII, ending on Necron alongside Maxor, Storm and Goldor, disciples of
     * The Wither King. The only floor whose Bedrock chest can hold a Necron's Handle.
     */
    @SerializedName("7")
    SEVEN(7, Size.LARGE, Boss.NECRON);

    /**
     * The floor number the wire uses as a map key, {@code 0} through {@code 7}.
     */
    @KeyField(strictKeys = true)
    private final int value;

    /**
     * How large the floor's room grid is.
     */
    private final @NotNull Size size;

    /**
     * The boss the floor ends on.
     */
    private final @NotNull Boss boss;

    /**
     * The boss that ends a Catacombs floor.
     * <p>
     * Several of them are also summoned as adds by The Watcher on later floors - Bonzo from floor 3
     * up, Scarf on 4 through 6, Livid on 6 and 7. Nothing binds this enum: it is constructor data on
     * {@link Floor} rather than a wire type, so the constant names are ours.
     */
    public enum Boss {

        /**
         * The Watcher, boss of the Entrance and the Blood Room mini-boss that gates the boss fight
         * on every later floor.
         */
        WATCHER,

        /**
         * Bonzo, the clown boss of floor 1.
         */
        BONZO,

        /**
         * Scarf, the undead summoner of floor 2.
         */
        SCARF,

        /**
         * The Professor, boss of floor 3.
         */
        THE_PROFESSOR,

        /**
         * Thorn, the ghast of floor 4, damageable only by his Spirit Bow.
         */
        THORN,

        /**
         * Livid, the cloning boss of floor 5.
         */
        LIVID,

        /**
         * Sadan, the Necromancer Lord of floor 6.
         */
        SADAN,

        /**
         * Necron, final boss of floor 7 alongside Maxor, Storm and Goldor.
         */
        NECRON;

        /**
         * Title-cased constant name with underscores as spaces, {@code THE_PROFESSOR} reading as
         * {@code The Professor}.
         */
        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

    }

    /**
     * How large a Catacombs floor's room grid is.
     * <p>
     * Bigger floors take longer to explore, which is why a run's speed allowance grows with the
     * floor. Nothing binds this enum either; it is constructor data on {@link Floor}.
     */
    public enum Size {

        /**
         * The Entrance and floor 1.
         */
        TINY,

        /**
         * Floors 2, 3 and 4.
         */
        SMALL,

        /**
         * Floors 5 and 6.
         */
        MEDIUM,

        /**
         * Floor 7.
         */
        LARGE;

        /**
         * Title-cased constant name, {@code MEDIUM} reading as {@code Medium}.
         */
        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name());
        }

    }

}
