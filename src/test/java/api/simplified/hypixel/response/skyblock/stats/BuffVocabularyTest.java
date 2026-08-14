package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Buff;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.persistence.JpaSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the refusals a buff row can only be checked for in this module's vocabulary against the
 * corpus as shipped.
 * <p>
 * {@link BuffValidator} is a pure function over loaded rows and no production path calls it, so
 * without this a misspelled name binds in silence: a variable no provider publishes, an expression
 * input nobody declares and a stat term naming no origin all load, evaluate and contribute nothing
 * forever, and no other test here would notice. Authoring a row is only safe once a wrong one fails
 * something.
 * <p>
 * This is the gate rather than a load hook, and the difference is deliberate. Nothing in this module
 * loads the corpus outside a test - it ships no client - so the read that can be guarded is the one
 * the build performs. A consumer that loads the corpus for itself guards its own read.
 *
 * @see BuffValidator
 */
class BuffVocabularyTest {

    private static JpaSession session;
    private static ConcurrentList<Buff> rows;

    @BeforeAll
    static void loadCorpus() {
        Optional<Path> corpus = LocalSkyBlockData.findCorpus();
        assumeTrue(corpus.isPresent(), "no skyblock checkout beside this module, and none named by -D" + LocalSkyBlockData.ROOT_PROPERTY);

        ConcurrentList<String> uncovered = LocalSkyBlockData.uncoveredModels(corpus.get());
        assumeTrue(uncovered.isEmpty(), "the reference models and the corpus are of different vintages - the corpus carries no file for " + uncovered);

        session = LocalSkyBlockData.connect(corpus.get());
        rows = SkyBlockData.getRepository(Buff.class).findAll();
    }

    @AfterAll
    static void releaseSession() {
        LocalSkyBlockData.disconnect(session);
        session = null;
        rows = null;
    }

    @Test
    @DisplayName("the table carries rows, so an empty read cannot pass the checks below")
    void theTableCarriesRows() {
        // every assertion here is over a list, and a list of nothing satisfies all of them - so the
        // count is what separates a clean corpus from one that never arrived
        assertThat(rows.size(), is(greaterThan(0)));
    }

    @Test
    @DisplayName("every name a shipped row spells in this module's vocabulary resolves")
    void everyLocalNameResolves() {
        assertThat(BuffValidator.corpus(rows), is(empty()));
    }

}
