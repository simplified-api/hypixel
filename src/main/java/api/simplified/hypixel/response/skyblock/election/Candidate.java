package api.simplified.hypixel.response.skyblock.election;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * One candidate standing in a SkyBlock mayor election.
 * <p>
 * A candidate carries the perks it would bring to office. The winner's perks all run for the term;
 * the runner-up becomes minister and contributes only the perk marked as its minister perk.
 * <p>
 * The wire spells those perks two ways. Every node but the sitting minister sends {@code perks} as
 * an array, and the minister sends the one perk it contributes as a lone {@code perk} object. Each
 * spelling binds to its own field so that the shape the wire chose is the shape it reads back as,
 * and {@link #getPerks()} reads whichever arrived.
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
     * Votes cast for this candidate on the ballot it stands on, and zero on the sitting mayor and
     * its minister, which the wire names outside any ballot.
     */
    private int votes;

    /**
     * Perks the wire lists as an array, which is how every node but the sitting minister spells
     * them.
     */
    @Getter(AccessLevel.NONE)
    @SerializedName("perks")
    private @NotNull ConcurrentList<Perk> perks = Concurrent.newList();

    /**
     * The lone perk the wire spells as a single object, which is how the sitting minister carries
     * the one perk it contributes.
     */
    @Getter(AccessLevel.NONE)
    @SerializedName("perk")
    private @NotNull Optional<Perk> perk = Optional.empty();

    /**
     * Perks this candidate brings to office, taken from whichever spelling the wire used and
     * holding a lone perk as a one-element list of its own.
     */
    public @NotNull ConcurrentList<Perk> getPerks() {
        if (this.perks.isEmpty() && this.perk.isPresent())
            return Concurrent.newUnmodifiableList(this.perk.get());

        return this.perks;
    }

    /**
     * The one perk that still runs when this candidate finishes runner-up, and empty on a candidate
     * whose perks are already in force, since the wire drops the flag where it means nothing.
     */
    public @NotNull Optional<Perk> getMinisterPerk() {
        return this.getPerks()
            .stream()
            .filter(Perk::isMinister)
            .findFirst();
    }

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
