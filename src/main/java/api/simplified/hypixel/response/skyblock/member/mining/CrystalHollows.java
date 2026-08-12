package api.simplified.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.gson.annotation.SerializedPath;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * A member's progress through the four Crystal Hollows biomes that carry per-visit state.
 *
 * <p>
 * Each of the four guards one Crystal Nucleus crystal behind its own task, and the wire records only
 * what has to survive a reset of the hollows themselves. The Fairy Grotto and the Magma Fields are
 * Crystal Hollows biomes with no node here, so this is not a map of every biome.
 *
 * <p>
 * Bound from {@code mining_core.biomes}, a key that gives no hint the value is this. Each of the
 * four is a non-null empty object when the wire omits it.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Crystal_Hollows">Crystal Hollows</a>
 */
@Getter
public class CrystalHollows {

    /**
     * State of the Mines of Divan, which the wire calls {@code dwarven} - not the Dwarven Mines,
     * which are a different location entirely.
     */
    @SerializedName("dwarven")
    private @NotNull MinesOfDivan minesOfDivan = new MinesOfDivan();

    /**
     * State of the Lost Precursor City, which the wire calls {@code precursor}.
     */
    @SerializedName("precursor")
    private @NotNull LostPrecursorCity lostPrecursorCity = new LostPrecursorCity();

    /**
     * State of the Goblin Holdout.
     */
    @SerializedPath("goblin")
    private @NotNull GoblinHoldout goblinHoldout = new GoblinHoldout();

    /**
     * State of the Jungle Temple.
     */
    @SerializedPath("jungle")
    private @NotNull JungleTemple jungleTemple = new JungleTemple();

    /**
     * The dwarven biome, where four Scavenged Items found with a Metal Detector are handed to their
     * Keepers to release the Jade Crystal.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Mines_of_Divan">Mines of Divan</a>
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MinesOfDivan {

        /**
         * The Keeper statues already given their Scavenged Item on this visit.
         *
         * <p>
         * The entries are whatever the wire puts in the array; it has only ever been seen empty, so
         * nothing pins the element shape down.
         */
        @SerializedName("statues_placed")
        private @NotNull ConcurrentList<Object> placedStatues = Concurrent.newList();

    }

    /**
     * The precursor biome, where six Automaton Parts are delivered to Professor Robot to release the
     * Sapphire Crystal.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Lost_Precursor_City">Lost Precursor City</a>
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class LostPrecursorCity {

        /**
         * The Automaton Parts already handed over on this visit.
         *
         * <p>
         * The entries are whatever the wire puts in the array; it has only ever been seen empty, so
         * nothing pins the element shape down.
         */
        @SerializedName("parts_delivered")
        private @NotNull ConcurrentList<Object> deliveredParts = Concurrent.newList();

    }

    /**
     * The goblin biome, where King Yolkar trades a Goblin Egg for the King's Scent effect that opens
     * the Goblin Queen's Den and its Amber Crystal.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Goblin_Holdout">Goblin Holdout</a>
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class GoblinHoldout {

        /**
         * Whether King Yolkar's egg request is open right now.
         */
        @SerializedName("king_quest_active")
        private boolean kingQuestActive;

        /**
         * Lifetime Goblin Eggs delivered to King Yolkar.
         */
        @SerializedName("king_quests_completed")
        private int completedKingQuests;

    }

    /**
     * The jungle biome, opened by giving a Kalhuiki Door Guardian a Jungle Key; the parkour inside
     * yields the Amethyst Crystal.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Jungle_Temple">Jungle Temple</a>
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JungleTemple {

        /**
         * Whether the temple door has been unlocked on this visit.
         */
        @SerializedName("jungle_temple_open")
        private boolean open;

        /**
         * Chest openings consumed inside the temple.
         */
        @SerializedName("jungle_temple_chest_uses")
        private int chestUses;

    }

}
