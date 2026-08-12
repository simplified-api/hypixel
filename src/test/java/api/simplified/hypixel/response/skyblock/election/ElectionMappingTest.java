package api.simplified.hypixel.response.skyblock.election;

import api.simplified.hypixel.response.resource.ResourceElection;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.simplified.gson.GsonSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Verifies the election response field mappings against a captured API response.
 * <p>
 * Every test decodes the whole {@link ResourceElection} rather than one subtree, because the two
 * perk spellings and the two ballots only appear together in the full document.
 */
class ElectionMappingTest {

    private static Gson gson;
    private static JsonObject pristine;

    @BeforeAll
    static void loadFixture() throws Exception {
        gson = GsonSettings.defaults().create();

        try (InputStream stream = ElectionMappingTest.class.getResourceAsStream("/elections.json")) {
            if (stream == null)
                throw new IllegalStateException("elections.json is missing from the classpath");

            pristine = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
            ).getAsJsonObject();
        }
    }

    private static ResourceElection decode() {
        return gson.fromJson(pristine.deepCopy(), ResourceElection.class);
    }

    @Test
    @DisplayName("both ballots bind with every candidate and its votes")
    void bindsBothBallots() {
        ResourceElection response = decode();

        Election won = response.getMayor().getElectionResults();
        assertThat(won.getYear(), is(equalTo(506)));
        assertThat(won.getCandidates().size(), is(equalTo(5)));
        assertThat(won.getCandidates().findFirst(Candidate::getKey, "slayer").orElseThrow().getVotes(),
            is(equalTo(876947)));

        Election current = response.getCurrentElection().orElseThrow();
        assertThat(current.getYear(), is(equalTo(507)));
        assertThat(current.getCandidates().size(), is(equalTo(5)));
        assertThat(current.getCandidates().getFirst().getKey(), is(equalTo("fishing")));
        assertThat(current.getCandidates().getFirst().getVotes(), is(equalTo(565048)));
    }

    @Test
    @DisplayName("the minister's lone perk object binds as a one-element perk list")
    void bindsTheMinistersLonePerk() {
        Candidate minister = decode().getMayor().getMinister().orElseThrow();

        assertThat(minister.getKey(), is(equalTo("events")));
        assertThat(minister.getName(), is(equalTo("Foxy")));
        assertThat(minister.getPerks().size(), is(equalTo(1)));
        assertThat(minister.getPerks().getFirst().getName(), is(equalTo("Sweet Benevolence")));
        assertThat(minister.getMinisterPerk().orElseThrow().getName(), is(equalTo("Sweet Benevolence")));
    }

    @Test
    @DisplayName("a node the wire sends no votes on reads zero")
    void votesAreZeroWhereTheWireSendsNone() {
        ResourceElection response = decode();

        // the sitting mayor and its minister are named outside any ballot, so neither carries a count
        assertThat(response.getMayor().getVotes(), is(equalTo(0)));
        assertThat(response.getMayor().getMinister().orElseThrow().getVotes(), is(equalTo(0)));
        assertThat(response.getMayor().getElectionResults().getCandidates().getFirst().getVotes(),
            is(not(equalTo(0))));
    }

    @Test
    @DisplayName("a sitting mayor's own perks carry no minister flag")
    void theSittingMayorsPerksCarryNoMinisterFlag() {
        Mayor mayor = decode().getMayor();

        assertThat(mayor.getPerks().size(), is(equalTo(2)));
        assertThat(mayor.getMinisterPerk().isPresent(), is(false));

        // the same candidate on the ballot it won does carry the flag, so the absence is the wire's
        // and not a lost mapping
        Candidate onTheBallot = mayor.getElectionResults()
            .getCandidates()
            .findFirst(Candidate::getKey, mayor.getKey())
            .orElseThrow();

        assertThat(onTheBallot.getMinisterPerk().orElseThrow().getName(), is(equalTo("Pathfinder")));
    }

    @Test
    @DisplayName("every candidate on a ballot marks exactly one minister perk")
    void everyBallotCandidateMarksOneMinisterPerk() {
        ResourceElection response = decode();

        Stream.concat(
            response.getMayor().getElectionResults().getCandidates().stream(),
            response.getCurrentElection().orElseThrow().getCandidates().stream()
        ).forEach(candidate -> assertThat(
            candidate.getPerks().stream().filter(Candidate.Perk::isMinister).count(),
            is(equalTo(1L))
        ));
    }

    @Test
    @DisplayName("an election is its year, not its ballot")
    void electionIdentityIsItsYear() {
        Election current = decode().getCurrentElection().orElseThrow();

        // Cycle declares no equals and a ballot is contents rather than a name, so folding either
        // into identity made two elections of one year unequal
        assertThat(current, is(equalTo(new Election(507))));
        assertThat(current.hashCode(), is(equalTo(new Election(507).hashCode())));
        assertThat(current.getCandidates(), is(not(empty())));
        assertThat(new Election(507).getCandidates(), is(empty()));
        assertThat(new Election(507), is(not(equalTo(new Election(508)))));
    }

    @Test
    @DisplayName("election cycles compute the same bounds the hook used to store")
    void computesElectionCycles() {
        Election election = new Election(278);

        // captured off the hook before it was removed, so these are the pre-change values rather
        // than a restatement of the expressions that now produce them
        assertThat(election.getVoting().getStart().getRealTime(), is(equalTo(1684145700000L)));
        assertThat(election.getVoting().getEnd().getRealTime(), is(equalTo(1684480500000L)));
        assertThat(election.getTerm().getStart().getRealTime(), is(equalTo(1684480500000L)));
        assertThat(election.getTerm().getEnd().getRealTime(), is(equalTo(1684926900000L)));

        // a term begins the moment voting closes
        assertThat(election.getTerm().getStart().getRealTime(),
            is(equalTo(election.getVoting().getEnd().getRealTime())));

        // the no-arg constructor leaves no half-built object: gson binds year, and both cycles
        // follow from it whenever they are asked for
        assertThat(new Election().getVoting().getStart().getRealTime(),
            is(not(equalTo(election.getVoting().getStart().getRealTime()))));
    }

    @Test
    @DisplayName("the round trip adds only the defaults the wire left out")
    void roundTripsBothPerkSpellings() {
        JsonObject out = JsonParser.parseString(gson.toJson(decode())).getAsJsonObject();
        JsonObject rawMayor = pristine.getAsJsonObject("mayor");
        JsonObject outMayor = out.getAsJsonObject("mayor");

        assertThat(out.get("lastUpdated"), is(equalTo(pristine.get("lastUpdated"))));
        assertThat(outMayor.getAsJsonObject("election"),
            is(equalTo(rawMayor.getAsJsonObject("election"))));
        assertThat(out.getAsJsonObject("current"), is(equalTo(pristine.getAsJsonObject("current"))));

        // the singular spelling goes back out singular, so the shape the wire chose is the shape it
        // reads back as; the two keys beside it are the non-null defaults of a node that sent neither
        JsonObject outMinister = outMayor.getAsJsonObject("minister");
        assertThat(outMinister.getAsJsonObject("perk"),
            is(equalTo(rawMayor.getAsJsonObject("minister").getAsJsonObject("perk"))));
        assertThat(outMinister.getAsJsonArray("perks").isEmpty(), is(true));
        assertThat(outMinister.get("votes").getAsInt(), is(equalTo(0)));

        // a perk the wire sent without the flag reads false and writes the false back
        JsonArray outPerks = outMayor.getAsJsonArray("perks");
        JsonArray rawPerks = rawMayor.getAsJsonArray("perks");
        assertThat(outPerks.size(), is(equalTo(rawPerks.size())));

        for (int index = 0; index < outPerks.size(); index++) {
            JsonObject outPerk = outPerks.get(index).getAsJsonObject();
            JsonObject rawPerk = rawPerks.get(index).getAsJsonObject();
            assertThat(outPerk.get("name"), is(equalTo(rawPerk.get("name"))));
            assertThat(outPerk.get("description"), is(equalTo(rawPerk.get("description"))));
            assertThat(rawPerk.has("minister"), is(false));
            assertThat(outPerk.get("minister").getAsBoolean(), is(false));
        }
    }

}
