package api.simplified.hypixel.response.skyblock.election;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The candidate holding the mayor's office, with its perks running for the whole term.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Mayor_Election">Mayor Election</a>
 */
@Getter
@NoArgsConstructor
public class Mayor extends Candidate {

    /**
     * Runner-up of the election, whose minister perk runs alongside the mayor's own perks, empty
     * when the wire reports no minister.
     */
    private @NotNull Optional<VotingCandidate> minister = Optional.empty();

    /**
     * Election that put this mayor in office, bound from the {@code election} node.
     */
    @SerializedName("election")
    private @NotNull Election electionResults = new Election();

}
