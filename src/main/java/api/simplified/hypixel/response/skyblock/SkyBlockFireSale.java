package api.simplified.hypixel.response.skyblock;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

/**
 * One cosmetic offered in a fire sale, sold for gems in limited stock over a fixed window.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Fire_Sale">Fire Sale</a>
 */
@Getter
public class SkyBlockFireSale {

    /**
     * Item id of the cosmetic on sale.
     */
    @SerializedName("item_id")
    private String itemId;

    /**
     * When the sale opens.
     */
    private SkyBlockDate.RealTime start;

    /**
     * When the sale closes, after which the cosmetic never returns.
     */
    private SkyBlockDate.RealTime end;

    /**
     * Copies stocked for the whole sale.
     */
    private int amount;

    /**
     * Gems one copy costs.
     */
    private int price;

}
