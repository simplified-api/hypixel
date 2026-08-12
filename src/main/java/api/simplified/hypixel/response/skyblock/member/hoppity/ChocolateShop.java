package api.simplified.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * A member's standing at the Chocolate Shop, which is reached through the Chocolate Factory menu.
 *
 * <p>
 * The shop sells a five-tier accessory, a cloak, chocolate fortune upgrades and a set of rabbits.
 * Most of its stock is limited per SkyBlock year, so the year the counters belong to is carried
 * alongside them. The whole node is absent for a member who never bought anything, and every field
 * here survives that.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Chocolate_Factory">Chocolate Factory</a>
 */
@Getter
public class ChocolateShop {

    /**
     * The SkyBlock year the yearly stock counters belong to.
     */
    private int year;

    /**
     * Rabbit ids the shop is currently offering.
     */
    private @NotNull ConcurrentList<String> rabbits = Concurrent.newList();

    /**
     * Chocolate spent in the shop.
     */
    @SerializedName("chocolate_spent")
    private long chocolateSpent;

    /**
     * Chocolate Fortune tiers bought, each granting one Cocoa Beans Fortune and capped at 25 in
     * total rather than per year. The game labels the upgrade Chocolate Fortune while the wire calls
     * it {@code cocoa_fortune_upgrades}.
     */
    @SerializedName("cocoa_fortune_upgrades")
    private int chocolateFortune;

    /**
     * Rabbit ids already bought this year - the receipt, where the offered rabbits are the menu. The
     * two lists hold the same ids in a different order once a member has bought everything offered.
     */
    @SerializedName("rabbits_purchased")
    private @NotNull ConcurrentList<String> rabbitsPurchased = Concurrent.newList();

}
