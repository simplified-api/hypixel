package api.simplified.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * One gemstone crystal the member has collected in the Crystal Hollows.
 *
 * <p>
 * Five of the twelve are the Crystal Nucleus set, carried back and placed in the nucleus to open its
 * treasure stash; the other seven are one-off collectibles that go towards forging perfect
 * gemstones. Bound from {@code mining_core.crystals.<type>}, one object per crystal, carrying the
 * crystal's current state and its lifetime found and placed counts.
 *
 * <p>
 * The wire sends an empty object for a crystal a member has never touched, so every value here falls
 * back to its default rather than being bound.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Gemstone_Crystals">Gemstone Crystals</a>
 */
@Getter
public class Crystal {

    /**
     * Whether the crystal is being carried right now.
     */
    private @NotNull State state = State.NOT_FOUND;

    /**
     * Lifetime placements of this crystal into the Crystal Nucleus.
     *
     * <p>
     * Only the five nucleus crystals ever carry this key, so a {@code 0} on any of the other seven
     * is the default rather than a count of placements that never happened.
     */
    @SerializedName("total_placed")
    private int totalPlaced;

    /**
     * Lifetime pickups of this crystal.
     *
     * <p>
     * The wire can send a crystal that is {@link State#FOUND} with no counter at all, and an absent
     * counter binds {@code 0} - absence and a genuine zero are indistinguishable here.
     */
    @SerializedName("total_found")
    private int totalFound;

    /**
     * Whether a crystal is in the member's hands.
     *
     * <p>
     * {@link #NOT_FOUND} is both the default and the resting state of a nucleus crystal: it flips to
     * {@link #FOUND} while the crystal is carried and back again once the crystal is placed.
     */
    public enum State {

        /**
         * The crystal is in the member's possession right now.
         */
        FOUND,

        /**
         * The crystal is not currently held - either never collected, or collected and already
         * placed.
         */
        NOT_FOUND

    }

    /**
     * The twelve gemstone crystals of the Crystal Hollows.
     *
     * <p>
     * The first five are the Crystal Nucleus set, each released by its own biome's task and then
     * placed in the nucleus; the remaining seven are collected once and never placed. Every constant
     * declares its own {@code <name>_crystal} wire name.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Gemstone_Crystals">Gemstone Crystals</a>
     */
    public enum Type {

        /**
         * Released in the Mines of Divan by finding four Scavenged Items with a Metal Detector and
         * handing each to its Keeper.
         */
        @SerializedName("jade_crystal")
        JADE,

        /**
         * Taken from the Goblin Queen's Den in the Goblin Holdout, entered under the King's Scent
         * effect that King Yolkar grants for a Goblin Egg.
         */
        @SerializedName("amber_crystal")
        AMBER,

        /**
         * Dropped after defeating Bal, beneath the Magma Fields.
         */
        @SerializedName("topaz_crystal")
        TOPAZ,

        /**
         * Released by handing the six Automaton Parts to Professor Robot in the Lost Precursor City.
         *
         * <p>
         * The constant name is a letter short of the gemstone's spelling; the wire name
         * {@code sapphire_crystal} is correct, so binding is unaffected.
         */
        @SerializedName("sapphire_crystal")
        SAPHIRE,

        /**
         * Awarded for completing the Jungle Temple parkour, entered by giving a Kalhuiki Door
         * Guardian a Jungle Key.
         */
        @SerializedName("amethyst_crystal")
        AMETHYST,

        /**
         * A rare drop from Fairy Grotto butterflies and chests, from the Crystal Nucleus treasure
         * stash, or from a Glacite Mineshaft.
         */
        @SerializedName("jasper_crystal")
        JASPER,

        /**
         * A very rare drop from mining ruby, from the Crystal Nucleus treasure stash, or from a
         * Glacite Mineshaft.
         */
        @SerializedName("ruby_crystal")
        RUBY,

        /**
         * Found in a Glacite Mineshaft of the matching type.
         */
        @SerializedName("onyx_crystal")
        ONYX,

        /**
         * Found in a Glacite Mineshaft of the matching type.
         */
        @SerializedName("aquamarine_crystal")
        AQUAMARINE,

        /**
         * Found in a Glacite Mineshaft of the matching type.
         */
        @SerializedName("opal_crystal")
        OPAL,

        /**
         * Found in a Glacite Mineshaft of the matching type.
         */
        @SerializedName("citrine_crystal")
        CITRINE,

        /**
         * Found in a Glacite Mineshaft of the matching type.
         */
        @SerializedName("peridot_crystal")
        PERIDOT

    }

}
