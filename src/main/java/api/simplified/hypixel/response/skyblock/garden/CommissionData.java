package api.simplified.hypixel.response.skyblock.garden;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

/**
 * Running totals of the visitors a profile's Garden has received and served.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Garden_Visitors">Garden Visitors</a>
 */
@Getter
@NoArgsConstructor
public class CommissionData {

    /**
     * Times each visitor has arrived at the Garden, keyed by the visitor's wire name.
     */
    private @NotNull ConcurrentMap<String, Integer> visits = Concurrent.newMap();

    /**
     * Offers completed for each visitor, keyed by the visitor's wire name.
     */
    private @NotNull ConcurrentMap<String, Integer> completed = Concurrent.newMap();

    /**
     * Offers completed across every visitor.
     */
    private int totalCompleted;

    /**
     * Distinct visitors the Garden has served at least once, bound from
     * {@code unique_npcs_served}.
     */
    @SerializedName("unique_npcs_served")
    private int uniqueNPCsServed;

}
