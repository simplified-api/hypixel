package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.SkyBlockIsland;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.gson.GsonSettings;
import dev.simplified.persistence.JpaSession;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the two properties the flat pass rests on: that the order its sources run in decides nothing,
 * and that a source writes only under its own origin.
 * <p>
 * Both are properties of the sink rather than of any one source. A source is handed a write face it
 * cannot read back, so it can neither observe what another source contributed nor write anywhere but
 * its own bucket. The golden file cannot see either one, because it records a single run in a single
 * order - so the compute is driven here from a given order instead.
 * <p>
 * A fresh member is decoded per compute. Two computes over one member are not independent: the
 * accessory bag memoises what it detected onto the member itself, so a second run would start from
 * the first one's leftovers rather than from the wire.
 */
class FlatPassOrderTest {

    private static JpaSession session;
    private static byte[] fixture;

    @BeforeAll
    static void openCorpus() throws Exception {
        Optional<Path> corpus = LocalSkyBlockData.findCorpus();
        assumeTrue(corpus.isPresent(), "no skyblock-data checkout beside this module, and none named by -D" + LocalSkyBlockData.ROOT_PROPERTY);
        ConcurrentList<String> uncovered = LocalSkyBlockData.uncoveredModels(corpus.get());
        assumeTrue(uncovered.isEmpty(), "the reference models and the corpus are of different vintages - the corpus carries no file for " + uncovered);
        session = LocalSkyBlockData.connect(corpus.get());

        try (InputStream stream = FlatPassOrderTest.class.getResourceAsStream("/craftedfury.json")) {
            if (stream == null)
                throw new IllegalStateException("craftedfury.json is missing from the classpath");

            fixture = stream.readAllBytes();
        }
    }

    @AfterAll
    static void releaseCorpus() {
        LocalSkyBlockData.disconnect(session);
        session = null;
    }

    @Test
    @DisplayName("the order the sources run in decides nothing")
    void theOrderTheSourcesRunInDecidesNothing() {
        ConcurrentList<StatSource> declared = Concurrent.newUnmodifiableList(StatSource.values());
        ConcurrentList<StatSource> reversed = Concurrent.newList();

        for (int index = declared.size() - 1; index >= 0; index--)
            reversed.add(declared.get(index));

        // an order that did not actually differ would pass this test while proving nothing
        assertThat(reversed.getFirst(), is(equalTo(declared.getLast())));
        assertThat(reversed.getLast(), is(equalTo(declared.getFirst())));

        Map<String, Double> expected = cells(compute(declared));

        // and so would two computes that each wrote nothing
        assertThat(expected.size(), is(greaterThan(100)));
        assertThat(cells(compute(reversed)), is(equalTo(expected)));
    }

    @Test
    @DisplayName("a source writes only under its own origin")
    void aSourceWritesOnlyUnderItsOwnOrigin() {
        int wrote = 0;

        for (StatSource source : StatSource.values()) {
            ProfileStats profileStats = compute(Concurrent.newUnmodifiableList(source));

            profileStats.getStats()
                .keySet()
                .forEach(origin -> assertThat(source + " wrote under " + origin, origin, is(equalTo(source))));

            if (!profileStats.getStats().isEmpty())
                wrote++;
        }

        // every source writing nothing would satisfy the loop above while proving nothing
        assertThat(wrote, is(greaterThan(10)));
    }

    private static @NotNull ProfileStats compute(@NotNull ConcurrentList<StatSource> sources) {
        Gson gson = GsonSettings.defaults().create();
        JsonObject root = JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(fixture), StandardCharsets.UTF_8)).getAsJsonObject();
        SkyBlockIsland island = gson.fromJson(root.getAsJsonArray("profiles").get(1), SkyBlockIsland.class);
        SkyBlockMember member = island.getMembers().stream().values().findFirst().orElseThrow();

        // the bonus pass is left off deliberately - it has an order of its own, and this is about the
        // pass that claims to have none
        return ProfileStats.compute(island, member, false, sources);
    }

    private static @NotNull Map<String, Double> cells(@NotNull ProfileStats profileStats) {
        Map<String, Double> cells = new TreeMap<>();

        profileStats.getStats().forEach((origin, statEntries) -> statEntries.forEach((statModel, data) -> {
            cells.put(String.format("%s/%s/base", origin.name(), statModel.getId()), data.getBase());
            cells.put(String.format("%s/%s/bonus", origin.name(), statModel.getId()), data.getBonus());
        }));

        return cells;
    }

}
