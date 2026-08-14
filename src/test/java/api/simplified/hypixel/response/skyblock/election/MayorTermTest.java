package api.simplified.hypixel.response.skyblock.election;

import api.simplified.skyblock.date.Season;
import api.simplified.skyblock.date.SkyBlockDate;
import dev.simplified.collection.ConcurrentList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Covers the mayor term cycles, which are derived from the year rather than stored.
 */
class MayorTermTest {

    @Test
    @DisplayName("election cycles compute the same bounds the hook used to store")
    void computesElectionCycles() {
        MayorTerm term = new MayorTerm(278);

        // captured before the hook was removed, so these are pre-change values rather than a
        // restatement of the expressions
        assertThat(term.getVoting().getStart().getRealTime(), is(equalTo(1684145700000L)));
        assertThat(term.getVoting().getEnd().getRealTime(), is(equalTo(1684480500000L)));
        assertThat(term.getTerm().getStart().getRealTime(), is(equalTo(1684480500000L)));
        assertThat(term.getTerm().getEnd().getRealTime(), is(equalTo(1684926900000L)));

        // a term begins the moment voting closes
        assertThat(term.getTerm().getStart().getRealTime(),
            is(equalTo(term.getVoting().getEnd().getRealTime())));
    }

    @Test
    @DisplayName("two elections of one year are equal and hash alike")
    void mayorTermIdentityIsItsYear() {
        // Cycle declares no equals, so folding the derived cycles into identity made this false
        assertThat(new MayorTerm(278), is(equalTo(new MayorTerm(278))));
        assertThat(new MayorTerm(278).hashCode(), is(equalTo(new MayorTerm(278).hashCode())));
        assertThat(new MayorTerm(278), is(not(equalTo(new MayorTerm(279)))));
    }

    @Test
    @DisplayName("a mayor term and a wire election of one year are not each other")
    void aTermAndAnElectionOfOneYearAreDistinct() {
        // identity is the year alone within a type, and the getClass guard keeps the two types
        // apart in both directions, which is what makes SpecialElection's own guard symmetric
        assertThat(new MayorTerm(507), is(not(equalTo(new Election(507)))));
        assertThat(new Election(507), is(not(equalTo(new MayorTerm(507)))));
        assertThat(new MayorTerm(507).hashCode(), is(equalTo(new Election(507).hashCode())));
    }

    @Test
    @DisplayName("a special election carries its mayor into identity")
    void specialElectionIdentityIncludesItsMayor() {
        assertThat(new SpecialElection(278, "JERRY_CANDIDATE"), is(equalTo(new SpecialElection(278, "JERRY_CANDIDATE"))));
        assertThat(new SpecialElection(278, "JERRY_CANDIDATE"), is(not(equalTo(new SpecialElection(278, "SHADY_CANDIDATE")))));
        assertThat(new SpecialElection(278, "JERRY_CANDIDATE").getVoting().getStart().getRealTime(),
            is(equalTo(1684145700000L)));
    }

    @Test
    @DisplayName("a term closes after the voting that opened it")
    void electionWindowsRunForward() {
        MayorTerm term = new MayorTerm(MayorTerm.FIRST_YEAR);
        SpecialElection special = new SpecialElection(
            SpecialElection.FIRST_YEAR,
            SpecialElection.mayorIdOf(SpecialElection.FIRST_YEAR)
        );

        assertThat(term.getVoting().getEnd().getRealTime(),
            is(greaterThan(term.getVoting().getStart().getRealTime())));
        assertThat(term.getTerm().getEnd().getRealTime(),
            is(greaterThan(term.getVoting().getEnd().getRealTime())));

        assertThat(special.getVoting().getEnd().getRealTime(),
            is(greaterThan(special.getVoting().getStart().getRealTime())));
        assertThat(special.getTerm().getEnd().getRealTime(),
            is(greaterThan(special.getVoting().getEnd().getRealTime())));
    }

    @Test
    @DisplayName("the special mayor rotation reproduces the years it has already run")
    void rotationMatchesTheRecordedHistory() {
        // the observed history, which is what fixes the rotation's phase
        assertThat(SpecialElection.mayorIdOf(96), is(equalTo("SHADY_CANDIDATE")));
        assertThat(SpecialElection.mayorIdOf(104), is(equalTo("DERP_CANDIDATE")));
        assertThat(SpecialElection.mayorIdOf(112), is(equalTo("JERRY_CANDIDATE")));
        assertThat(SpecialElection.mayorIdOf(120), is(equalTo("SHADY_CANDIDATE")));
        assertThat(SpecialElection.mayorIdOf(136), is(equalTo("JERRY_CANDIDATE")));
        assertThat(SpecialElection.mayorIdOf(144), is(equalTo("SHADY_CANDIDATE")));

        // year 128 stood outside the rotation, which is why the overrides are carried beside it
        assertThat(SpecialElection.mayorIdOf(128), is(equalTo("DANTE_CANDIDATE")));
        assertThat(SpecialElection.ROTATION, hasItem(SpecialElection.mayorIdOf(120)));
        assertThat(SpecialElection.ROTATION, not(hasItem(SpecialElection.mayorIdOf(128))));
    }

    @Test
    @DisplayName("a forecast starts at the given year and never behind the first election")
    void upcomingCountsForwardFromTheDate() {
        ConcurrentList<? extends MayorTerm> fromLaunch = MayorTerm.upcoming(3, new SkyBlockDate(1, Season.EARLY_SPRING, 1, 0));

        // one election a year, and none before the year voting first opened
        assertThat(fromLaunch.size(), is(equalTo(3)));
        assertThat(fromLaunch.getFirst().getYear(), is(equalTo(MayorTerm.FIRST_YEAR)));
        assertThat(fromLaunch.getLast().getYear(), is(equalTo(MayorTerm.FIRST_YEAR + 2)));

        ConcurrentList<? extends MayorTerm> fromLater = MayorTerm.upcoming(2, new SkyBlockDate(278, Season.EARLY_SPRING, 1, 0));
        assertThat(fromLater.getFirst().getYear(), is(equalTo(278)));

        // a count below one still answers with one
        assertThat(MayorTerm.upcoming(0, new SkyBlockDate(278, Season.EARLY_SPRING, 1, 0)).size(), is(equalTo(1)));
    }

    @Test
    @DisplayName("special elections forecast every eighth year with the mayor of that year")
    void upcomingSpecialElectionsStepThePeriod() {
        ConcurrentList<SpecialElection> upcoming = SpecialElection.upcoming(3, new SkyBlockDate(1, Season.EARLY_SPRING, 1, 0));

        assertThat(upcoming.getFirst().getYear(), is(equalTo(SpecialElection.FIRST_YEAR)));
        assertThat(upcoming.get(1).getYear(), is(equalTo(SpecialElection.FIRST_YEAR + SpecialElection.PERIOD_YEARS)));
        assertThat(upcoming.getLast().getYear(), is(equalTo(SpecialElection.FIRST_YEAR + (SpecialElection.PERIOD_YEARS * 2))));

        // each carries the mayor the rotation reaches for its own year
        for (SpecialElection election : upcoming)
            assertThat(election.getSpecialMayorId(), is(equalTo(SpecialElection.mayorIdOf(election.getYear()))));
    }

}
