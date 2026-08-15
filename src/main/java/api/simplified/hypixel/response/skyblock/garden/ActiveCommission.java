package api.simplified.hypixel.response.skyblock.garden;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * One offer a visitor is currently standing at the Garden with.
 * <p>
 * The wire calls these commissions; in game they are the trades Garden visitors ask for in return
 * for farming and Garden experience, copper and bits.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Garden_Visitors">Garden Visitors</a>
 */
@Getter
@NoArgsConstructor
public class ActiveCommission {

    /**
     * Items the visitor is asking for, bound from the singular {@code requirement} key.
     */
    @SerializedName("requirement")
    private @NotNull ConcurrentList<Requirement> requirements = Concurrent.newList();

    /**
     * How far the offer has been taken.
     */
    private @NotNull Status status = Status.NOT_STARTED;

    /**
     * Place this visitor holds in the queue waiting at the Garden.
     */
    private int position;

    /**
     * One item and amount a visitor asks for.
     */
    @Getter
    @NoArgsConstructor
    public static class Requirement {

        /**
         * Item the offer was first rolled with.
         */
        @SerializedName("original_item")
        private @NotNull String baselineItem;

        /**
         * Amount the offer was first rolled with.
         */
        @SerializedName("original_amount")
        private int baselineAmount;

        /**
         * Item the visitor is asking for now.
         */
        @SerializedName("item")
        private @NotNull String askedItem;

        /**
         * Amount the visitor is asking for now.
         */
        @SerializedName("amount")
        private int askedAmount;

    }

    /**
     * States an offer can be reported in.
     */
    public enum Status {

        /**
         * Offer standing at the Garden with nothing handed in against it yet.
         */
        NOT_STARTED

    }

}
