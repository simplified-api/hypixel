package api.simplified.hypixel.response.skyblock.election;

import api.simplified.skyblock.date.Season;
import api.simplified.skyblock.date.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * One SkyBlock mayor election, identified by the year it runs in.
 * <p>
 * Both windows are derived on demand from {@code year} rather than bound, which is what lets gson
 * bind the no-arg constructor without ever leaving a half-built object behind. Identity is the year
 * alone - {@link Cycle} declares no equality of its own, so folding the derived cycles in would make
 * two elections of the same year unequal.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Mayor_Election">Mayor Election</a>
 */
@Getter
@NoArgsConstructor
public class Election {

    /**
     * SkyBlock year this election runs in.
     */
    private int year;

    /**
     * Constructs an election for one SkyBlock year.
     *
     * @param year the SkyBlock year the election runs in
     */
    public Election(int year) {
        this.year = year;
    }

    /**
     * Window in which this election's candidates are voted on, opening late summer 27 of its year
     * and closing late spring 27 of the next.
     */
    public @NotNull Cycle getVoting() {
        return new Cycle(
            new SkyBlockDate(this.getYear(), Season.LATE_SUMMER, 27, 0),
            new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0)
        );
    }

    /**
     * Window in which the elected mayor holds office, running from the close of voting for a full
     * year, so its start is exactly the end of the voting window.
     */
    public @NotNull Cycle getTerm() {
        return new Cycle(
            new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0),
            new SkyBlockDate(this.getYear() + 2, Season.LATE_SPRING, 27, 0)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Election election = (Election) o;

        return this.getYear() == election.getYear();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getYear());
    }

    @Override
    public String toString() {
        return String.format("Election{year=%d, voting=%s, term=%s}", this.getYear(), this.getVoting(), this.getTerm());
    }

    /**
     * A pair of SkyBlock dates bounding one phase of an election.
     */
    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Cycle {

        /**
         * Date the phase opens.
         */
        private final @NotNull SkyBlockDate start;

        /**
         * Date the phase closes.
         */
        private final @NotNull SkyBlockDate end;

        @Override
        public String toString() {
            return String.format("Cycle{start=%s, end=%s}", this.getStart(), this.getEnd());
        }

    }

}
