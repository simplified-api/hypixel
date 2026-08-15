package api.simplified.hypixel.response.skyblock.member.mining;

import api.simplified.hypixel.response.skyblock.member.SkillTree;
import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NamingStyle;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A member's Heart of the Mountain - the mining skill tree fed by Dwarven Mines, Crystal Hollows and
 * Glacite Tunnels progress, abbreviated HotM in game.
 *
 * <p>
 * Bound from {@code mining_core}, it holds the three powder balances, the gemstone crystals
 * collected so far, the per-biome Crystal Hollows state, the Sky Mall daily effect and the daily
 * first-ore counters.
 *
 * <p>
 * <b>The perk tree itself is not here.</b> Perk levels and their toggles, the tree's experience, the
 * tokens sunk into it, when it was last reset and the equipped pickaxe ability are all held on the
 * member's {@link SkillTree}, reached through {@link SkillTree.Tree#MINING}.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Heart_of_the_Mountain">Heart of the Mountain</a>
 */
@Getter
public class HeartOfTheMountain {

    /**
     * Whether the free first tier of the tree has been granted.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("received_free_tier")
    private boolean hasReceivedFreeTier;

    /**
     * The Sky Mall buff rolled for the current SkyBlock day.
     *
     * <p>
     * Sky Mall is a tier 4 perk that rolls one of six buffs a day: mining speed, mining fortune,
     * powder gain, pickaxe ability cooldown, golden and diamond goblin chance, or titanium drops.
     */
    @SerializedName("current_daily_effect")
    private Optional<String> currentSkymallEffect = Optional.empty();

    /**
     * The SkyBlock day number on which the Sky Mall buff last rerolled.
     *
     * <p>
     * A day number, not an epoch stamp, in spite of sitting beside millisecond values on this same
     * class.
     */
    @SerializedName("current_daily_effect_last_changed")
    private int skymallEffectLastChanged;

    /**
     * Per-biome Crystal Hollows state.
     *
     * <p>
     * The wire key is {@code biomes} and gives no hint that the value is a {@link CrystalHollows}.
     */
    @SerializedName("biomes")
    private @NotNull CrystalHollows crystalHollows = new CrystalHollows();

    // Time

    /**
     * When the member last entered the Crystal Hollows.
     *
     * <p>
     * {@code greater_mines} is the older name for the hollows; there is no separate mine behind this
     * key.
     */
    @SerializedName("greater_mines_last_access")
    private @NotNull Optional<SkyBlockDate.RealTime> lastAccessToGreaterMines = Optional.empty();

    // Tokens

    /**
     * Unspent Tokens of the Mountain.
     */
    @SerializedName("tokens")
    private int remainingTokens;

    /**
     * Whether the back-dated tier 2 token grant has been applied.
     */
    @SerializedName("retroactive_tier2_token")
    private boolean retroactiveTier2Token;

    /**
     * Whether an in-game notice that a migration reset the tree is still queued for the member.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("hotm_migrator_tree_reset_send_message")
    private boolean hasPendingTreeResetMessage;

    /**
     * The gemstone crystals collected in the Crystal Hollows, keyed by {@link Crystal.Type}.
     *
     * <p>
     * A member who has never been to the hollows has an empty map, and one who has may still be
     * missing entries for crystals never seen.
     */
    private @NotNull ConcurrentMap<Crystal.Type, Crystal> crystals = Concurrent.newMap();

    // Powder

    /**
     * The three powder balances, keyed by {@link Powder.Type}.
     *
     * <p>
     * Captured rather than named: every key on the node beginning {@code powder_} is folded in here,
     * the prefix is stripped, and what remains is grouped by the affixes {@link Powder} declares.
     * The remainder of the key is what names the type.
     */
    @Capture(filter = "^powder_")
    private @NotNull ConcurrentMap<Powder.Type, Powder> powder = Concurrent.newMap();

    // Daily Ores

    /**
     * Ores mined today across all three regions, towards the daily first-ore powder bonus.
     */
    @SerializedName("daily_ores_mined")
    private int dailyOresMined;

    /**
     * The SkyBlock day the combined ore counter belongs to.
     *
     * <p>
     * A day number older than today means the count beside it has not been rolled over yet, so a
     * {@code 0} there is a leftover rather than today's tally.
     */
    @SerializedName("daily_ores_mined_day")
    private int dailyOresMinedDay;

    /**
     * Ores mined today in the Dwarven Mines, towards the mithril powder first-ore bonus.
     */
    @SerializedName("daily_ores_mined_mithril_ore")
    private int dailyOresMinedMithrilOre;

    /**
     * The SkyBlock day the Dwarven Mines ore counter belongs to.
     */
    @SerializedName("daily_ores_mined_day_mithril_ore")
    private int dailyOresMinedDayMithrilOre;

    /**
     * Ores mined today in the Crystal Hollows, towards the gemstone powder first-ore bonus.
     */
    @SerializedName("daily_ores_mined_gemstone")
    private int dailyOresMinedGemstone;

    /**
     * The SkyBlock day the Crystal Hollows ore counter belongs to.
     */
    @SerializedName("daily_ores_mined_day_gemstone")
    private int dailyOresMinedDayGemstone;

    /**
     * Ores mined today in the Glacite Tunnels, towards the glacite powder first-ore bonus.
     */
    @SerializedName("daily_ores_mined_glacite")
    private int dailyOresMinedGlacite;

    /**
     * The SkyBlock day the Glacite Tunnels ore counter belongs to.
     */
    @SerializedName("daily_ores_mined_day_glacite")
    private int dailyOresMinedDayGlacite;

}
