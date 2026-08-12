package api.simplified.hypixel.response.skyblock.election;

import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * An election together with the five candidates standing in it and the votes each has drawn.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Mayor_Election">Mayor Election</a>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class VotingBooth extends Election {

    /**
     * Candidates standing in this election, each carrying its own running vote count.
     */
    private @NotNull ConcurrentList<VotingCandidate> candidates = Concurrent.newList();

}
