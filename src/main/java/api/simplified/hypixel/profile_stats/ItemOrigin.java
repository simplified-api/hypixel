package api.simplified.hypixel.profile_stats;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The eight sources an item's stats are split between.
 * <p>
 * An accessory is an item, so one enum covers both - the buckets an accessory never fills simply stay
 * unwritten. Every bucket but the reforge is fixed for the item, so an optimiser totals them once; a
 * reforge is the one thing it is free to change.
 */
@Getter
@RequiredArgsConstructor
public enum ItemOrigin implements StatOrigin {

    /**
     * Health from the New Year Cake Bag, one point for each cake stored in it.
     */
    CAKE_BAG(true),

    /**
     * Stats from enchantments, counting only those that apply against anything.
     */
    ENCHANTS(true),

    /**
     * The single stat an accessory enrichment adds.
     */
    ENRICHMENTS(true),

    /**
     * Stats from the gemstones slotted into the item.
     */
    GEMSTONES(true),

    /**
     * Stats from hot potato books, scaled by how many the item carries.
     */
    HOT_POTATOES(true),

    /**
     * Stats from the applied reforge, scaled by the item's rarity.
     */
    REFORGES(false),

    /**
     * Stats the item provides in its own right.
     */
    STATS(true),

    /**
     * The flat bonuses from The Art of War and The Art of Peace.
     */
    SUN_TZU(true);

    /**
     * Whether this bucket is fixed for the item, so an optimiser need not recompute it.
     */
    private final boolean optimizerConstant;

}
