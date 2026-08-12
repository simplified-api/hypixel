package api.simplified.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * A member's dealings with Enigma, who trades for the Enigma Souls hidden across the Rift.
 *
 * <p>
 * Enigma's Crib is a sub-location of the Wyld Woods. Buying Enigma's cloak is the one-off purchase
 * that opens the trade, and souls handed in claim bonuses off a ladder. The wire key on the member
 * is {@code enigma}.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Enigma's_Crib">Enigma's Crib</a>
 */
@Getter
public class EnigmasCrib {

    /**
     * Whether Enigma's cloak has been bought, bound from {@code bought_cloak}.
     */
    @Accessors(fluent = true)
    @SerializedName("bought_cloak")
    private boolean hasBoughtCloak;

    /**
     * Ids of the Enigma Souls collected, bound from {@code found_souls}. Souls from every area land
     * in this one list, and the wire's spelling survives verbatim - the ids are mostly upper case
     * but not uniformly so, and nothing here normalises them, so a caller comparing ids has to fold
     * case itself.
     */
    @SerializedName("found_souls")
    private @NotNull ConcurrentList<String> foundSouls = Concurrent.newList();

    /**
     * How far up the soul-count bonus ladder has been claimed, bound from
     * {@code claimed_bonus_index}. This is an index into that ladder rather than a count of souls.
     */
    @SerializedName("claimed_bonus_index")
    private int claimedBonusIndex;

}
