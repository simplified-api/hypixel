package api.simplified.hypixel.response.skyblock.stats.buff;

import api.simplified.skyblock.model.Buff;
import api.simplified.skyblock.model.Stat;
import com.google.gson.Gson;
import dev.simplified.collection.Concurrent;
import dev.simplified.gson.GsonSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

/**
 * Pins that a rule's scope decides which table folds it.
 * <p>
 * A scope belongs to a rule rather than to a row, so one row reaches two tables and each caller folds
 * only the half addressed to it. Nothing in the corpus carries a scope today, which is exactly why
 * this is the only thing behind the column - the fold is where it takes effect and the fold is what
 * is driven here.
 */
class BuffScopeTest {

    private static final Gson GSON = GsonSettings.defaults().create();
    private static final double EPSILON = 1.0E-6;

    private static final @NotNull Stat STRENGTH = GSON.fromJson("{\"id\":\"STRENGTH\",\"multiplicable\":true}", Stat.class);

    /**
     * One row reaching both tables at once - the shape the column exists for.
     */
    private static final @NotNull String SPLIT = """
        { "rules": [ { "scope": "CARRIER", "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 5 } },
                     { "scope": "PROFILE", "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 100 } } ] }
        """;

    @Test
    @DisplayName("a scope decides which table folds a rule, and each table folds only its own")
    void aScopeDecidesWhichTableFoldsARule() {
        BuffEvaluator evaluator = compile(SPLIT);

        // the carrier's table takes the carrier rule and leaves the profile rule alone
        assertThat(fold(evaluator, Buff.Rule.Scope.CARRIER, 0.0), is(closeTo(5.0, EPSILON)));

        // and the member's table takes the other one, from the same row and the same evaluator
        assertThat(fold(evaluator, Buff.Rule.Scope.PROFILE, 0.0), is(closeTo(100.0, EPSILON)));

        // asking for neither folds both, which is what every caller holding the only table in play
        // wants - and is what every caller did before a rule could name a second table
        assertThat(
            evaluator.apply(STRENGTH, Buff.Channel.VALUE, Buff.Rule.Stage.BONUS, 0.0, context()),
            is(closeTo(105.0, EPSILON))
        );
    }

    @Test
    @DisplayName("a row is asked whether it writes a scope before its table is walked")
    void aRowIsAskedWhetherItWritesAScope() {
        assertThat(compile(SPLIT).writes(Buff.Rule.Scope.PROFILE), is(true));
        assertThat(compile(SPLIT).writes(Buff.Rule.Scope.CARRIER), is(true));

        // an unscoped rule is a carrier rule, so a row that never says the word writes no profile
        // rule - which is what keeps the member's table from being walked once per carried row
        BuffEvaluator carrierOnly = compile("""
            { "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """);

        assertThat(carrierOnly.writes(Buff.Rule.Scope.CARRIER), is(true));
        assertThat(carrierOnly.writes(Buff.Rule.Scope.PROFILE), is(false));
    }

    @Test
    @DisplayName("an effects entry folds at the carrier scope alone, so it is not contributed twice")
    void anEffectsEntryFoldsAtTheCarrierScopeAlone() {
        // effects carries no scope of its own and is shorthand for a default-scoped add, so a row
        // holding one beside a profile rule would otherwise add it to both tables
        BuffEvaluator evaluator = compile("""
            { "effects": { "STRENGTH": 7 },
              "rules": [ { "scope": "PROFILE", "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 100 } } ] }
            """);

        assertThat(fold(evaluator, Buff.Rule.Scope.CARRIER, 0.0), is(closeTo(7.0, EPSILON)));
        assertThat(fold(evaluator, Buff.Rule.Scope.PROFILE, 0.0), is(closeTo(100.0, EPSILON)));

        // and the two together are the effect once, not twice
        assertThat(
            evaluator.apply(STRENGTH, Buff.Channel.VALUE, Buff.Rule.Stage.BONUS, 0.0, context()),
            is(closeTo(107.0, EPSILON))
        );
    }

    @Test
    @DisplayName("a rule reads the half it names and writes the bonus one")
    void aRuleReadsTheHalfItNamesAndWritesTheBonusOne() {
        // base 100, bonus 20, so the three halves read 100, 20 and 120

        // BONUS is the default and the simple case - double what the bonuses came to
        assertThat(cell(scaled("BONUS", 2), 100.0, 20.0), is(closeTo(40.0, EPSILON)));

        // TOTAL doubles the whole cell and writes the difference to the bonus, so the total it
        // produces is the total a whole-cell rescale would produce - 240 - and only the split differs
        double bonus = cell(scaled("TOTAL", 2), 100.0, 20.0);
        assertThat(bonus, is(closeTo(140.0, EPSILON)));
        assertThat(100.0 + bonus, is(closeTo(240.0, EPSILON)));

        // BASE reads what the source gave and lands its change on the bonus, leaving the base alone
        assertThat(cell(scaled("BASE", 2), 100.0, 20.0), is(closeTo(120.0, EPSILON)));
    }

    @Test
    @DisplayName("an add does not depend on the half it reads")
    void anAddDoesNotDependOnTheHalfItReads() {
        // what an add contributes is not a function of what it is added to, so all three halves
        // agree - which is why the operation declares itself not half-bearing
        for (String half : new String[] { "BASE", "BONUS", "TOTAL" }) {
            String row = String.format("""
                { "rules": [ { "half": "%s", "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 7 } } ] }
                """, half);

            assertThat(half, cell(compile(row), 100.0, 20.0), is(closeTo(27.0, EPSILON)));
        }
    }

    private static @NotNull BuffEvaluator scaled(@NotNull String half, int factor) {
        return compile(String.format("""
            { "rules": [ { "half": "%s", "operation": "MULTIPLY", "target": { "kind": "ALL" },
                           "value": { "kind": "NUMBER", "amount": %d } } ] }
            """, half, factor));
    }

    private static double cell(@NotNull BuffEvaluator evaluator, double base, double bonus) {
        return evaluator.applyToCell(STRENGTH, Buff.Channel.VALUE, Buff.Rule.Stage.BONUS, null, base, bonus, context());
    }

    private static double fold(@NotNull BuffEvaluator evaluator, @NotNull Buff.Rule.Scope scope, double current) {
        return evaluator.apply(STRENGTH, Buff.Channel.VALUE, Buff.Rule.Stage.BONUS, scope, current, context());
    }

    private static @NotNull BuffEvaluator.Context context() {
        return BuffEvaluator.Context.of(Concurrent.newMap());
    }

    private static @NotNull BuffEvaluator compile(@NotNull String row) {
        return BuffEvaluator.compile(GSON.fromJson(row, Buff.class));
    }

}
