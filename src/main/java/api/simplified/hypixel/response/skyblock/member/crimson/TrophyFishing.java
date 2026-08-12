package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.PairOptional;
import dev.simplified.gson.annotation.Capture;
import dev.simplified.gson.annotation.Split;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * A member's trophy fishing record - every fish counted by tier, a grand total, and the last catch.
 * <p>
 * Trophy fish are pulled out of the Crimson Isle's lava and filleted for Magmafish. The wire node is a
 * sibling of the island's node on the member rather than a child of it, even though this class sits in
 * the same package as the island's tree.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Trophy_Fish">Trophy Fish</a>
 */
@Getter
public class TrophyFishing {

    /**
     * Every trophy fish the member has ever caught, at any tier.
     */
    @SerializedName("total_caught")
    private int totalCaught;

    /**
     * The reward indexes the member has claimed, in the order the wire lists them. They are neither
     * sequential nor a count, and nothing here says what they index.
     */
    @SerializedName("rewards")
    private @NotNull ConcurrentList<Integer> rewards = Concurrent.newList();

    /**
     * The most recent catch, split by {@link Split} out of the wire's delimited string into the fish
     * and the tier it came up at.
     */
    @Split("/")
    @SerializedName("last_caught")
    private @NotNull PairOptional<TrophyFish, TrophyFish.Tier> lastCaught = PairOptional.empty();

    /**
     * Catch counts per fish. The wire has no nesting at all - a fish's total and its four tier counts
     * are five sibling keys beside the declared fields - and the {@link Capture} regroups them into one
     * object per fish. A fish the member has never caught sends no keys and is simply absent rather
     * than present with zeros.
     */
    @Capture
    private @NotNull ConcurrentMap<TrophyFish, TierData> fish = Concurrent.newMap();

    /**
     * One fish's catch counts, split into the four trophy tiers plus the total across them.
     * <p>
     * The tiers are exclusive grades rather than cumulative ones - a Gold catch is not also counted as
     * Bronze - and the total is their sum, so it is redundant with them. It is kept because it is what
     * the wire sends and it is the base key the whole grouping hangs off.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Trophy_Fish">Trophy Fish</a>
     */
    @Getter
    @NoArgsConstructor
    public static class TierData {

        /**
         * Every catch of this fish at any tier. The empty {@link SerializedName} means the bare base
         * key itself rather than no name; every other field here is an auto-suffix on that base.
         */
        @SerializedName("")
        private int total;

        /**
         * Catches at Bronze.
         */
        private int bronze;

        /**
         * Catches at Silver.
         */
        private int silver;

        /**
         * Catches at Gold.
         */
        private int gold;

        /**
         * Catches at Diamond.
         */
        private int diamond;

    }

}
