package api.simplified.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Lenient;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * A member's Chocolate Factory - the idle clicker that runs all year and feeds off Hoppity's Hunt.
 *
 * <p>
 * Chocolate accrues every second from seven hired rabbit employees and a stack of multipliers, and
 * is spent on those employees, on upgrades and in the Chocolate Shop. Prestiging to the next factory
 * level resets every upgrade except the rabbit barn.
 *
 * <p>
 * Bound from the wire's {@code events.easter} node, which is the whole feature - no
 * {@code chocolate_factory} key exists anywhere on the wire. A member who barely touched the factory
 * sends a handful of these keys and nothing else, so every field survives absence.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Chocolate_Factory">Chocolate Factory</a>
 */
@Getter
public class ChocolateFactory {

    // Chocolate

    /**
     * Chocolate currently held and spendable, which is a small fraction of what has been earned.
     */
    private long chocolate;

    /**
     * All-time chocolate earned, never reset by a prestige.
     */
    @SerializedName("total_chocolate")
    private long totalChocolate;

    /**
     * Chocolate earned since the last factory level-up, which is what the prestige thresholds are
     * measured against.
     */
    @SerializedName("chocolate_since_prestige")
    private long chocolateSincePrestige;

    /**
     * The prestige tier, 1 through 6, raising the base chocolate-per-second multiplier, the employee
     * level cap and the chocolate storage cap, and unlocking rarer rabbits.
     */
    @SerializedName("chocolate_level")
    private int chocolateLevel;

    /**
     * When the factory menu was last opened; offline production accrues from it.
     */
    @SerializedName("last_viewed_chocolate_factory")
    private Instant lastViewed;

    // Collection

    /**
     * Sort order last selected in the Hoppity's Collection menu, defaulting to
     * {@link RabbitSort#A_TO_Z} for a member who never opened it.
     */
    @SerializedName("rabbit_sort")
    private @NotNull RabbitSort rabbitSort = RabbitSort.A_TO_Z;

    /**
     * Found and unfound filter last selected in the Hoppity's Collection menu, defaulting to
     * {@link RabbitFilter#NONE} for a member who never opened it.
     */
    @SerializedName("rabbit_filter")
    private @NotNull RabbitFilter rabbitFilter = RabbitFilter.NONE;

    /**
     * Island filter selected on the egg hotspot view, kept as a raw string because the island
     * vocabulary is open-ended.
     *
     * <p>
     * The wire key really is {@code rabbit_hotspot_filer}. That misspelling is Hypixel's, it is the
     * only spelling any response carries, and binding depends on matching it - repairing it to
     * {@code rabbit_hotspot_filter} silently binds nothing. The wire sends lowercase values while
     * the absent case falls back to the uppercase {@code "NONE"}, so the two casings do not agree.
     */
    @SerializedName("rabbit_hotspot_filer")
    private @NotNull String rabbitHotspot = "NONE";

    // Rabbits

    /**
     * Level of each hired rabbit employee. Lookup on the key is case-insensitive, but no
     * {@link RabbitEmployee} constant is a fallback, so an employee id Hypixel adds later binds a
     * {@code null} key rather than being dropped.
     */
    private @NotNull ConcurrentMap<RabbitEmployee, Integer> employees = Concurrent.newMap();

    /**
     * The rabbit barn upgrade, which bounds how many unique rabbits the collection can hold and is
     * the one upgrade a prestige does not reset.
     */
    @SerializedName("rabbit_barn_capacity_level")
    private int barnCapacity;

    /**
     * How many times the El Dorado golden rabbit has been caught; the third catch adds the legendary
     * El Dorado rabbit to the collection.
     */
    @SerializedName("el_dorado_progress")
    private int elDoradoProgress;

    /**
     * How many times each rabbit has been found, keyed by rabbit id, where anything past the first
     * find is a duplicate sold for chocolate.
     *
     * <p>
     * Marked {@link Lenient}, so the two entries the wire nests here that are not counts -
     * {@code collected_eggs} and {@code collected_locations} - fall into overflow instead of failing
     * the bind, and the two {@link Extract} fields below each claim one back out of it.
     */
    @Lenient
    private @NotNull ConcurrentMap<String, Integer> rabbits = Concurrent.newMap();

    /**
     * Per-meal counter for the six chocolate egg meals, lifted out of the rabbit counts rather than
     * bound from a root key of its own, so nothing here is emitted outside {@code rabbits} on a
     * write.
     */
    @Extract("rabbits.collected_eggs")
    private @NotNull ConcurrentMap<String, Long> eggs = Concurrent.newMap();

    /**
     * Egg location ids found on each island, keyed by the wire's internal island id rather than the
     * island's display name, and lifted out of the rabbit counts rather than bound from a root key
     * of its own.
     */
    @Extract("rabbits.collected_locations")
    private @NotNull ConcurrentMap<String, ConcurrentList<String>> locations = Concurrent.newMap();

    // Golden Rabbits

    /**
     * How many Golden Click bonuses are running; each adds five chocolate per second for the rest of
     * the SkyBlock year and they stack.
     */
    @SerializedName("golden_click_amount")
    private int goldenClickAmount;

    /**
     * The SkyBlock year the running Golden Clicks expire with.
     */
    @SerializedName("golden_click_year")
    private int goldenClickYear;

    // Upgrades

    /**
     * Hand-Baked Chocolate level, capped at 10, adding one chocolate per manual click per level -
     * the only upgrade that raises what a click is worth.
     */
    @SerializedName("click_upgrades")
    private int clickUpgrades;

    /**
     * The Time Tower upgrade and the charges banked on it.
     */
    @SerializedName("time_tower")
    private @NotNull ChocolateTimeTower timeTower = new ChocolateTimeTower();

    /**
     * Rabbit Shrine level, each level adding two percent to the chance of a higher-rarity rabbit
     * during Hoppity's Hunt.
     */
    @SerializedName("rabbit_rarity_upgrades")
    private int rabbitShrine;

    /**
     * Coach Jackrabbit level, each level adding a permanent {@code 0.01x} to chocolate per second -
     * unlike {@link ChocolateTimeTower}, which multiplies only while a charge is running.
     */
    @SerializedName("chocolate_multiplier_upgrades")
    private int coachJackrabbit;

    /**
     * Hoppity's hitman and the uncollected eggs it is holding. The wire spells the key plural.
     */
    @SerializedName("rabbit_hitmen")
    private @NotNull RabbitHitman hitman = new RabbitHitman();

    // Shop

    /**
     * The member's standing at the Chocolate Shop.
     */
    private @NotNull ChocolateShop shop = new ChocolateShop();

    /**
     * Supreme Chocolate Bars bought in bulk from the shop and not yet redeemed.
     */
    @SerializedName("supreme_chocolate_bars")
    private int remainingSupremeChocolateBars;

    /**
     * Refined Dark Cacao Truffles bought in bulk from the shop and not yet redeemed.
     */
    @SerializedName("refined_dark_cacao_truffles")
    private int remainingDarkCacaoTruffles;

}
