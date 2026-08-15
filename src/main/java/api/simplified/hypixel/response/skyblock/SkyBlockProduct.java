package api.simplified.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * One product traded on the Bazaar, with both sides of its order book and a summary of each.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Bazaar">Bazaar</a>
 */
@Getter
public class SkyBlockProduct {

    /**
     * Item id the product trades under, which is also its key in the Bazaar snapshot.
     */
    @SerializedName("product_id")
    private @NotNull String itemId;

    /**
     * Buy side of the order book, one entry per price level.
     */
    @SerializedName("buy_summary")
    private @NotNull ConcurrentList<Summary> buySummary = Concurrent.newList();

    /**
     * Sell side of the order book, one entry per price level.
     */
    @SerializedName("sell_summary")
    private @NotNull ConcurrentList<Summary> sellSummary = Concurrent.newList();

    /**
     * Both sides of the book reduced to a handful of figures, empty rather than null when the wire
     * omits them.
     */
    @SerializedName("quick_status")
    private @NotNull Status quickStatus = new Status();

    /**
     * A product's order book reduced to a price, a standing volume and a week's throughput per side.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Bazaar">Bazaar</a>
     */
    @Getter
    public static class Status {

        /**
         * Item id the figures belong to.
         */
        private String productId;

        /**
         * Coins per unit at the top of the sell side.
         */
        private double sellPrice;

        /**
         * Units standing on the sell side.
         */
        private long sellVolume;

        /**
         * Units that moved through the sell side over the last seven days.
         */
        private long sellMovingWeek;

        /**
         * Orders standing on the sell side.
         */
        private long sellOrders;

        /**
         * Coins per unit at the top of the buy side.
         */
        private double buyPrice;

        /**
         * Units standing on the buy side.
         */
        private long buyVolume;

        /**
         * Units that moved through the buy side over the last seven days.
         */
        private long buyMovingWeek;

        /**
         * Orders standing on the buy side.
         */
        private long buyOrders;

    }

    /**
     * One price level of an order book, pooling every order placed at that price.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Bazaar">Bazaar</a>
     */
    @Getter
    public static class Summary {

        /**
         * Units standing at this price level.
         */
        private long amount;

        /**
         * Coins one unit costs at this price level.
         */
        private double pricePerUnit;

        /**
         * Orders pooled into this price level.
         */
        @SerializedName("orders")
        private int numberOfOrders;

    }

}
