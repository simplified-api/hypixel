package api.simplified.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * The contents of the Rift Gallery - the timecharms a member has secured in the dimension's museum.
 *
 * <p>
 * There are eight timecharms. Each one secured grants SkyBlock experience, opens access to a wider
 * range of zones and raises the stats on the Rift Necklace. Elise runs the gallery and hands the
 * necklace over once the Supreme Timecharm is donated. This models the gallery's contents rather
 * than the location, and it binds from the wire key {@code gallery}.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Rift_Gallery">Rift Gallery</a>
 */
@Getter
public class TimecharmGallery {

    /**
     * How far Elise's dialogue chain has run, bound from {@code elise_step}.
     */
    @SerializedName("elise_step")
    private int eliseStep;

    /**
     * The timecharms secured in the gallery, bound from {@code secured_trophies} - trophy is the
     * wire's word for what the game calls a timecharm.
     */
    @SerializedName("secured_trophies")
    private @NotNull ConcurrentList<Trophy> securedTrophies = Concurrent.newList();

    /**
     * Ids of the timecharms Elise has already commented on, bound from
     * {@code sent_trophy_dialogues}. This is dialogue bookkeeping keyed by the same ids as the
     * secured timecharms, not a second record of what was donated.
     */
    @SerializedName("sent_trophy_dialogues")
    private @NotNull ConcurrentList<String> sentTrophyDialogues = Concurrent.newList();

    /**
     * One timecharm secured in the Rift Gallery.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Rift_Timecharms">Rift Timecharms</a>
     */
    @Getter
    public static class Trophy {

        /**
         * The timecharm's wire id, lowercase.
         */
        private String type;

        /**
         * When the timecharm was secured, bound from raw epoch milliseconds.
         */
        private Instant timestamp;

        /**
         * The member's lifetime Rift visit count at the moment this timecharm was secured - not a
         * count of visits to the timecharm.
         */
        private int visits;

    }

}
