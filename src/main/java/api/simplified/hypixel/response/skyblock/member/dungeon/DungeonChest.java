package api.simplified.hypixel.response.skyblock.member.dungeon;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.gson.annotation.SerializedPath;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * One post-boss reward chest that has not been claimed yet.
 * <p>
 * Three chests always spawn at the end of a Catacombs run and up to three more for an exceptional
 * score. Each costs coins to open, only one may be claimed per run unless a Dungeon Chest Key is
 * used, and a chest may be rerolled with a Kismet Feather. Whatever is left unclaimed sits with
 * Croesus for 72 hours. No chest spawns in the Entrance, so a chest never belongs to a
 * {@link Floor#ENTRANCE} run.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Dungeon_Reward_Chest">Dungeon Reward Chest</a>
 */
@Getter
public class DungeonChest {

    /**
     * Uuid of the run this chest spawned from, matching the id of that {@link DungeonRun}.
     * <p>
     * Carries no default, unlike almost everything else here, so a chest the wire sent no run id for
     * binds null against the contract.
     */
    @SerializedName("run_id")
    private @NotNull UUID runId;

    /**
     * Uuid of the chest itself, with no default of its own.
     */
    @SerializedName("chest_id")
    private @NotNull UUID chestId;

    /**
     * Which of the six tiers this chest spawned at.
     */
    @SerializedName("treasure_type")
    private @NotNull Type type;

    /**
     * The chest's loot quality - the currency the loot roller spends against each item's quality
     * cost, seeded from the floor's base quality and buffed by the Treasure accessories, the Boss
     * Luck perk and the Catacombs Box attribute.
     * <p>
     * <b>Not the quality that actually rolled the loot.</b> Upstream applies those modifiers once
     * into this number and then again using it as an input, so what the wire reports is one round of
     * modifiers short of the value the roller used.
     */
    private int quality;

    /**
     * The wire's shiny flag on this chest.
     * <p>
     * Shiny is a master mode chance on certain rewards - a Necron's Handle has a 5% shiny chance
     * there - and gives the weapon crafted from the drop extra cosmetic effects.
     */
    @SerializedName("shiny_eligible")
    private boolean shinyEligible;

    /**
     * Whether the chest's coin cost has been settled, paid out of the purse first, then the personal
     * bank, then a co-op bank.
     */
    private boolean paid;

    /**
     * How many times the chest has been rerolled with a Kismet Feather.
     */
    private int rerolls;

    /**
     * Ids of the items inside the chest, read from a nested wire node.
     */
    @SerializedPath("rewards.rewards")
    private @NotNull ConcurrentList<String> items = Concurrent.newList();

    /**
     * Whether the RNG Meter drop in this chest came up by chance rather than from a filled meter,
     * read from the same nested wire node the items are.
     * <p>
     * A Catacombs RNG Meter fills on the Dungeon Score of S and S+ runs and guarantees the selected
     * drop once full.
     */
    @SerializedPath("rewards.rolled_rng_meter_randomly")
    private boolean rolledRngMeterRandomly;

    /**
     * The six tiers a dungeon reward chest can spawn at.
     */
    public enum Type {

        /**
         * The Wood chest.
         */
        WOOD,

        /**
         * The Gold chest.
         */
        GOLD,

        /**
         * The Diamond chest.
         */
        DIAMOND,

        /**
         * The Emerald chest.
         */
        EMERALD,

        /**
         * The Obsidian chest.
         */
        OBSIDIAN,

        /**
         * The Bedrock chest, the only one that can hold a Necron's Handle and only on floor 7.
         */
        BEDROCK

    }

}
