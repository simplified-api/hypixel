package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Flatten;
import org.jetbrains.annotations.NotNull;

/**
 * The spendable balances a member carries.
 *
 * <p>
 * Bits and gems are not on this node - bits come from a Booster Cookie and gems are bought outside
 * the game, so neither is a member value.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Coins">Coins</a>
 */
@Getter
public class Currencies {

    /**
     * Motes held, the Rift's own currency.
     */
    @SerializedName("motes_purse")
    private int motes;

    /**
     * Coins carried in the purse, a {@code double} because coins really do carry decimals.
     */
    @SerializedName("coin_purse")
    private double purse;

    /**
     * Essence held per essence type. The wire wraps each amount in a single-key {@code current}
     * object, which is reduced to the value here and wrapped again on write.
     */
    @Flatten("current")
    private @NotNull ConcurrentMap<String, Integer> essence = Concurrent.newMap();

}