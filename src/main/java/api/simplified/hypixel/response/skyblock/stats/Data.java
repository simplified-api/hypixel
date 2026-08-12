package api.simplified.hypixel.response.skyblock.stats;

import lombok.Getter;

/**
 * One stat's value on one source, kept as a base and a bonus rather than a single number.
 * <p>
 * The two are tracked apart because most of what raises a stat in SkyBlock is multiplicative on the
 * base alone - a reforge or an enchantment scales what the item itself provides, and folding the two
 * together first would scale the additions as well.
 */
public class Data {

    /**
     * Value the source provides on its own, before anything modifies it.
     */
    @Getter double base;

    /**
     * Value added on top by reforges, gemstones, enrichments and item bonuses.
     */
    @Getter double bonus;

    /**
     * Constructs a new {@code Data} with both halves at zero.
     */
    public Data() { }

    /**
     * Both halves added together.
     */
    public final double getTotal() {
        return this.getBase() + this.getBonus();
    }

}
