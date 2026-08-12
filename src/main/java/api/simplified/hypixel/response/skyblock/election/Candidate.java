package api.simplified.hypixel.response.skyblock.election;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * One candidate standing in a SkyBlock mayor election.
 * <p>
 * A candidate carries the perks it would bring to office. The winner's perks all run for the term;
 * the runner-up becomes minister and contributes only the perk marked as its minister perk.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Mayor_Election">Mayor Election</a>
 */
@Getter
@NoArgsConstructor
public class Candidate {

    /**
     * Wire id of the candidate.
     */
    private @NotNull String key = "";

    /**
     * Display name of the candidate.
     */
    private @NotNull String name = "";

    /**
     * Perks this candidate would bring to office, bound from {@code perks} or from the singular
     * {@code perk} the wire spells it with elsewhere.
     */
    @SerializedName(alternate = "perk", value = "perks")
    private @NotNull ConcurrentList<Perk> perks = Concurrent.newList();

    /**
     * One perk a candidate brings to office.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    public static class Perk {

        /**
         * Display name of the perk.
         */
        private @NotNull String name = "";

        /**
         * In-game description of what the perk grants.
         */
        private @NotNull String description = "";

        /**
         * Whether this is the candidate's minister perk, the one perk that still runs when the
         * candidate finishes runner-up instead of winning.
         */
        private boolean minister;

    }

}
