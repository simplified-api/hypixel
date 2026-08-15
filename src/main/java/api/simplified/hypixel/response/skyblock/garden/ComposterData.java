package api.simplified.hypixel.response.skyblock.garden;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;

/**
 * The Garden composter's stored inputs, its finished compost and its upgrade levels.
 * <p>
 * The composter consumes organic matter and machine fuel continuously, producing one compost at a
 * time.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Composter">Composter</a>
 */
@Getter
@NoArgsConstructor
public class ComposterData {

    /**
     * Organic matter stored and waiting to be consumed.
     */
    @SerializedName("organic_matter")
    private double organicMatter;

    /**
     * Machine fuel stored and waiting to be consumed.
     */
    @SerializedName("fuel_units")
    private double fuelUnits;

    /**
     * Compost produced and waiting to be collected, counted in units.
     */
    @SerializedName("compost_units")
    private int compostUnits;

    /**
     * Compost produced and waiting to be collected, counted in whole items.
     */
    @SerializedName("compost_items")
    private int compostItems;

    /**
     * Ticks of progress made toward the compost currently being produced.
     */
    @SerializedName("conversion_ticks")
    private int conversionTicks;

    /**
     * Real time the server last wrote the composter's state.
     */
    @SerializedName("last_save")
    private SkyBlockDate.RealTime lastSave;

    /**
     * Upgrade levels bought for the composter.
     */
    private Upgrades upgrades = new Upgrades();

    /**
     * Levels bought for each of the composter's five upgrades.
     */
    @Getter
    @NoArgsConstructor
    public static class Upgrades {

        /**
         * Composter Speed level, raising production speed by 20% a tier.
         */
        private int speed;

        /**
         * Multi Drop level, granting a 3% chance a tier of producing one extra compost.
         */
        @SerializedName("multi_drop")
        private int multiDrop;

        /**
         * Fuel Cap level, raising the machine fuel the composter can hold by 30,000 a tier.
         */
        @SerializedName("fuel_cap")
        private int fuelCap;

        /**
         * Organic Matter Cap level, raising the organic matter the composter can hold by 30,000 a
         * tier.
         */
        @SerializedName("organic_matter_cap")
        private int organicMatterCap;

        /**
         * Cost Reduction level, lowering the organic matter and machine fuel one compost costs by
         * 1% a tier.
         */
        @SerializedName("cost_reduction")
        private int costReduction;

    }

}
