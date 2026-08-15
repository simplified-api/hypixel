package api.simplified.hypixel.response.skyblock;

import api.simplified.hypixel.common.NbtContent;
import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * One member's museum - the items they have donated and what the donations are worth.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Museum">Museum</a>
 */
@Getter
public class SkyBlockMuseum {

    /**
     * Coin value of everything donated.
     */
    private long value;

    /**
     * Whether the value has been appraised rather than taken at the items' base worth.
     */
    private boolean appraisal;

    /**
     * Each donation keyed by the museum slot it fills.
     */
    private @NotNull ConcurrentMap<String, Item> items = Concurrent.newMap();

    /**
     * Donations to the special section, which has no fixed slots to key them by.
     */
    @SerializedName("special")
    private @NotNull ConcurrentList<Item> specialItems = Concurrent.newList();

    /**
     * A single donation held in the museum.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Museum">Museum</a>
     */
    @Getter
    public static class Item {

        /**
         * When the donation was made.
         */
        @SerializedName("donated_time")
        private SkyBlockDate.RealTime donated;

        /**
         * Whether the member currently has the item out of the museum.
         */
        private boolean borrowing;

        /**
         * Slot on the featured wall the donation is displayed in, absent when it is not on display.
         */
        @SerializedName("featured_slot")
        private @NotNull Optional<String> featuredSlot = Optional.empty();

        /**
         * The donated item stacks, carried as base64 NBT rather than as JSON.
         */
        private @NotNull NbtContent items = new NbtContent();

        /**
         * Whether the donation is on display, which is to say it holds a featured slot.
         */
        public boolean isFeatured() {
            return this.featuredSlot.isPresent();
        }

        /**
         * Whether the donation is off display, the inverse of {@link #isFeatured()}.
         */
        public boolean notFeatured() {
            return !this.isFeatured();
        }

    }

}
