package api.simplified.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * A member's progress in the West Village, the Rift's mirror of the Village.
 *
 * <p>
 * Reaching it takes the Supreme Timecharm and 3:00 of remaining Rift Time. It is the busiest node in
 * the Rift subtree: four unrelated sub-features each get a holder of their own - Unhinged Kloon's
 * terminal hack, the Mirrorverse, Kat the Neuroscientist's house, and the glyph chain.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/West_Village">West Village</a>
 */
@Getter
public class WestVillage {

    /**
     * Unhinged Kloon's terminal hack, bound from {@code crazy_kloon}.
     */
    @SerializedName("crazy_kloon")
    private @NotNull CrazyKloon crazyKloon = new CrazyKloon();

    /**
     * The Mirrorverse rooms visited and what was claimed there.
     */
    private @NotNull Mirrorverse mirrorverse = new Mirrorverse();

    /**
     * The vermin cleared out of Kat the Neuroscientist's house, bound from {@code kat_house}.
     */
    @SerializedName("kat_house")
    private @NotNull KatHouse katHouse = new KatHouse();

    /**
     * The village's glyph chain.
     */
    private @NotNull Glyphs glyphs = new Glyphs();

    /**
     * The terminal hack Unhinged Kloon asks for in the West Village.
     *
     * <p>
     * The Retro-Encabulating Visor reveals wires, each leading to one of eight terminals; all eight
     * must be hacked and then set to the rainbow of colours facing Kloon. The wire key is
     * {@code crazy_kloon} while the NPC's own name is Unhinged Kloon.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Unhinged_Kloon">Unhinged Kloon</a>
     */
    @Getter
    public static class CrazyKloon {

        /**
         * The colour chosen at each terminal, bound from {@code selected_colors}. Terminal names
         * are the English number words {@code one} through {@code eight} in lower case and the
         * colour values are upper case.
         */
        @SerializedName("selected_colors")
        private @NotNull ConcurrentMap<String, String> selectedColors = Concurrent.newMap();

        /**
         * Whether Kloon has been spoken to, bound from {@code talked}.
         */
        @Accessors(fluent = true)
        @SerializedName("talked")
        private boolean hasTalked;

        /**
         * Names of the terminals hacked, bound from {@code hacked_terminals} - the same English
         * number words, in the arbitrary order the wire sends them.
         */
        @SerializedName("hacked_terminals")
        private @NotNull ConcurrentList<String> hackedTerminals = Concurrent.newList();

        /**
         * Whether the hack is finished, bound from {@code quest_complete}.
         */
        @SerializedName("quest_complete")
        private boolean questComplete;

    }

    /**
     * The Mirrorverse, one of the places Rift Time does not tick down.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Mirrorverse">Mirrorverse</a>
     */
    @Getter
    public static class Mirrorverse {

        /**
         * Names of the rooms visited, bound from {@code visited_rooms}. These are human-readable
         * names carrying spaces and hyphens rather than ids, so nothing can look one up.
         */
        @SerializedName("visited_rooms")
        private @NotNull ConcurrentList<String> visitedRooms = Concurrent.newList();

        /**
         * Whether the hard variant of the upside-down parkour has been completed, bound from the
         * shorter wire key {@code upside_down_hard}.
         */
        @SerializedName("upside_down_hard")
        private boolean upsideDownHardCompleted;

        /**
         * Ids of the items claimed from the Mirrorverse chests, bound from
         * {@code claimed_chest_items}. Unlike the room names these are real item ids.
         */
        @SerializedName("claimed_chest_items")
        private @NotNull ConcurrentList<String> claimedChestItems = Concurrent.newList();

        /**
         * Whether the Mirrorverse reward has been claimed, bound from {@code claimed_reward}.
         */
        @SerializedName("claimed_reward")
        private boolean claimedReward;

    }

    /**
     * The vermin vacuumed into bins at Kat the Neuroscientist's infested house in the West Village.
     *
     * <p>
     * This is the Rift's Kat, not the Hub's pet care NPC. The wire reports the same three counts a
     * second time under the member's Rift statistics; the two agree because the wire sends both,
     * not because either is computed from the other.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Kat_(Rift)">Kat (Rift)</a>
     */
    @Getter
    public static class KatHouse {

        /**
         * Mosquitoes binned, bound from {@code bin_collected_mosquito}.
         */
        @SerializedName("bin_collected_mosquito")
        private int collectedMosquito;

        /**
         * Spiders binned, bound from {@code bin_collected_spider}.
         */
        @SerializedName("bin_collected_spider")
        private int collectedSpider;

        /**
         * Silverfish binned, bound from {@code bin_collected_silverfish}.
         */
        @SerializedName("bin_collected_silverfish")
        private int collectedSilverfish;

    }

    /**
     * The West Village glyph chain.
     *
     * <p>
     * Two of the flags are one word apart and mean different things - {@code current_glyph_completed}
     * is per glyph, while the bare {@code completed} is the whole chain.
     */
    @Getter
    public static class Glyphs {

        /**
         * Whether the chain's wand has been claimed, bound from {@code claimed_wand}.
         */
        @SerializedName("claimed_wand")
        private boolean claimedWand;

        /**
         * Whether the current glyph has been delivered, bound from
         * {@code current_glyph_delivered}.
         */
        @SerializedName("current_glyph_delivered")
        private boolean currentGlyphDelivered;

        /**
         * Whether the current glyph is finished, bound from {@code current_glyph_completed} - per
         * glyph, not the whole chain.
         */
        @SerializedName("current_glyph_completed")
        private boolean currentGlyphCompleted;

        /**
         * Which glyph the chain is on, bound from {@code current_glyph}. The counter is not wound
         * back when the chain finishes.
         */
        @SerializedName("current_glyph")
        private int currentGlyph;

        /**
         * Whether the whole glyph chain is finished - the one key in the object that carries no
         * prefix, and the field name already matches it.
         */
        private boolean completed;

        /**
         * Whether the chain's bracelet has been claimed, bound from {@code claimed_bracelet}.
         */
        @SerializedName("claimed_bracelet")
        private boolean claimedBracelet;

    }

}
