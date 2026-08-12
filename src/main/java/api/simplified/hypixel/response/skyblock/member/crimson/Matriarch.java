package api.simplified.hypixel.response.skyblock.member.crimson;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;

/**
 * A member's standing with The Matriarch, the creature that swallows players in the Belly of the Beast
 * and lets them take Heavy Pearls while they are inside.
 * <p>
 * Three pearls regenerate every 24 hours, and attributes and consumables can lift a single visit's take
 * above that, so the base allotment is a floor rather than a cap. Unusually for this package the class
 * carries no non-null contract at all.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/The_Matriarch">The Matriarch</a>
 */
@Getter
public class Matriarch {

    /**
     * Heavy Pearls the member took on their most recent visit. The wire key reads like a lifetime
     * total and is not one.
     */
    @SerializedName("pearls_collected")
    private int lastCollectedPearls;

    /**
     * When the member was last eaten, and {@code null} on a member who never has been - it has no
     * default, and a null date field is what stops a whole member from serializing.
     */
    @SerializedName("last_attempt")
    private SkyBlockDate.RealTime lastAttempt;

    /**
     * Timestamps of the member's recent pearl refreshes, and the only field here with a non-null
     * default.
     */
    @SerializedName("recent_refreshes")
    private ConcurrentList<SkyBlockDate.RealTime> recentRefreshes = Concurrent.newList();

}
