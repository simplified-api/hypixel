package api.simplified.hypixel.response.skyblock.election;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A candidate as it stands in one election, carrying the votes cast for it.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Mayor_Election">Mayor Election</a>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class VotingCandidate extends Candidate {

    /**
     * Votes cast for this candidate in the election it stands in.
     */
    private int votes;

}
