package api.simplified.hypixel.response.skyblock.election;

import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * One SkyBlock mayor election as the wire reports it, carrying the candidates standing on its
 * ballot.
 * <p>
 * The year and both calendar windows are the {@link MayorTerm} the election runs in, and identity
 * comes from there too: an election is its year, so the inherited {@code equals} reads nothing else
 * and two elections of one year stay equal however their ballots differ. Declaring equality here
 * again is what would fold the ballot in.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Mayor_Election">Mayor Election</a>
 */
@Getter
@NoArgsConstructor
public class Election extends MayorTerm {

    /**
     * Candidates standing on this election's ballot, each carrying the votes cast for it, and empty
     * on an election named by its year alone.
     */
    private @NotNull ConcurrentList<Candidate> candidates = Concurrent.newList();

    /**
     * Constructs a new {@code Election} for the given SkyBlock year.
     *
     * @param year the SkyBlock year the election runs in
     */
    public Election(int year) {
        super(year);
    }

    @Override
    public String toString() {
        return String.format(
            "Election{year=%d, candidates=%d, voting=%s, term=%s}",
            this.getYear(),
            this.getCandidates().size(),
            this.getVoting(),
            this.getTerm()
        );
    }

}
