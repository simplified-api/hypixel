package api.simplified.hypixel.response.skyblock.election;

import api.simplified.skyblock.date.Season;
import api.simplified.skyblock.date.SkyBlockDate;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.AllArgsConstructor;
import dev.simplified.annotations.EqualsAndHashCode;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.annotations.RequiredArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * One turn of the SkyBlock mayor calendar, identified by the year it runs in.
 *
 * <p>
 * Voting opens on late summer 27 of year N and closes on late spring 27 of year N+1. The winning
 * mayor's term runs from that close for a full SkyBlock year, so {@code term.start} is exactly
 * {@code voting.end}. Both windows are computed on demand from the year, which is what lets gson
 * bind the no-argument constructor without leaving a half-built object behind.
 *
 * <p>
 * Identity is the year alone. The derived cycles stay outside {@code equals} and {@code hashCode} -
 * a {@link Cycle} declares no equality of its own, so folding them in would make two terms of one
 * year compare unequal. The guard is {@code getClass()} rather than {@code instanceof}, so a term
 * and a subclass of it are never equal in either direction however their years line up, and
 * {@link SpecialElection}'s own guard stays symmetric against it.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Mayor_Election">Mayor Election</a>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MayorTerm {

    /**
     * SkyBlock year mayor voting first opened in, and the earliest year a term runs in.
     */
    public static final int FIRST_YEAR = 88;

    /**
     * SkyBlock year this term runs in, and the whole of its identity.
     */
    private int year;

    /**
     * Window in which this term's votes are cast, opening late summer 27 of its year and closing
     * late spring 27 of the next.
     */
    public @NotNull Cycle getVoting() {
        return new Cycle(
            new SkyBlockDate(this.getYear(), Season.LATE_SUMMER, 27, 0),
            new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0)
        );
    }

    /**
     * Window in which the elected mayor holds office, running from the close of voting for a full
     * SkyBlock year.
     */
    public @NotNull Cycle getTerm() {
        return new Cycle(
            new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0),
            new SkyBlockDate(this.getYear() + 2, Season.LATE_SPRING, 27, 0)
        );
    }

    /**
     * Finds the next term due from the current time.
     *
     * @return the earliest term not behind the current SkyBlock year
     */
    public static @NotNull MayorTerm next() {
        return upcoming(1).getFirst();
    }

    /**
     * Collects the terms due from the current time.
     *
     * @param next how many terms to return, clamped to at least one
     * @return the upcoming terms, in calendar order
     */
    public static @NotNull ConcurrentList<? extends MayorTerm> upcoming(int next) {
        return upcoming(next, new SkyBlockDate(System.currentTimeMillis()));
    }

    /**
     * Collects the terms due from the given date.
     *
     * <p>
     * One term runs every SkyBlock year from {@link #FIRST_YEAR} onwards, and any whose year
     * precedes the given date is skipped.
     *
     * <p>
     * The result is a forecast to read rather than a list to add to, which is why its element type
     * is bounded: {@link SpecialElection} answers the same question about the years it stands in and
     * returns its own kind.
     *
     * @param next how many terms to return, clamped to at least one
     * @param fromDate the date to start counting from
     * @return the upcoming terms, in calendar order
     */
    public static @NotNull ConcurrentList<? extends MayorTerm> upcoming(int next, @NotNull SkyBlockDate fromDate) {
        next = Math.max(next, 1);
        ConcurrentList<MayorTerm> terms = Concurrent.newList();

        for (int year = Math.max(FIRST_YEAR, fromDate.getYear()); terms.size() < next; year++)
            terms.add(new MayorTerm(year));

        return terms;
    }

    @Override
    public String toString() {
        return String.format("MayorTerm{year=%d, voting=%s, term=%s}", this.getYear(), this.getVoting(), this.getTerm());
    }

    /**
     * A span of the SkyBlock calendar, given as the two dates that bound it.
     *
     * <p>
     * A cycle carries no equality of its own, so two cycles are distinct even when their
     * endpoints are the same date.
     */
    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Cycle {

        /**
         * Date the window opens on.
         */
        private final @NotNull SkyBlockDate start;

        /**
         * Date the window closes on.
         */
        private final @NotNull SkyBlockDate end;

        @Override
        public String toString() {
            return String.format("Cycle{start=%s, end=%s}", this.getStart(), this.getEnd());
        }

    }

}
