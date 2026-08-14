package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.stats.buff.BuffEvaluator;
import api.simplified.skyblock.model.Buff;
import api.simplified.skyblock.model.Stat;
import com.google.gson.Gson;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.GsonSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

/**
 * Pins that a count reaches a condition, and that an unanswered world property leaves a rule alone.
 * <p>
 * Both were silently inert until the compute started answering them, which is the failure this is
 * here to stop coming back: a rule reading an unfilled property folds nothing, with no throw and no
 * failing test, so the row looks authored and contributes zero.
 */
class ComputeFactsTest {

    private static final Gson GSON = GsonSettings.defaults().create();
    private static final double EPSILON = 1.0E-6;

    private static final @NotNull Stat STRENGTH = GSON.fromJson("{\"id\":\"STRENGTH\",\"multiplicable\":true}", Stat.class);

    /**
     * The shape of a four-piece set bonus - the second of the eight cap raises the design records.
     */
    private static final @NotNull String FOUR_PIECES = """
        { "conditions": [ { "input": { "kind": "COUNT", "group": "YOUNG_DRAGON" }, "op": "GTE", "amount": 4 } ],
          "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 100 } } ] }
        """;

    @Test
    @DisplayName("a count gates a set bonus, and a set worn short of its threshold contributes nothing")
    void aCountGatesASetBonus() {
        assertThat(fold(FOUR_PIECES, counted(4)), is(closeTo(100.0, EPSILON)));
        assertThat(fold(FOUR_PIECES, counted(5)), is(closeTo(100.0, EPSILON)));

        // three pieces is a row that evaluated and declined, which is the point of counting a group
        // rather than filtering one out before it is looked at
        assertThat(fold(FOUR_PIECES, counted(3)), is(closeTo(0.0, EPSILON)));
        assertThat(fold(FOUR_PIECES, counted(0)), is(closeTo(0.0, EPSILON)));
    }

    @Test
    @DisplayName("a count nobody answered leaves the rule inert rather than reading zero")
    void aCountNobodyAnsweredLeavesTheRuleInert() {
        // an unanswered group and a group counted zero are different states, and only one of them is
        // a member wearing none of a set - the other is nobody having asked
        assertThat(fold(FOUR_PIECES, Concurrent.newMap()), is(closeTo(0.0, EPSILON)));

        // naming a different group is the same silence, which is what a misspelled group id buys
        assertThat(fold(FOUR_PIECES, counted("THERMODYNAMIC", 4)), is(closeTo(0.0, EPSILON)));
    }

    @Test
    @DisplayName("a world property the compute cannot answer leaves its rule folding nothing")
    void aWorldPropertyTheComputeCannotAnswerLeavesItsRuleFoldingNothing() {
        // MODE is answered and ZONE never is, so the same shape of row is live under one and inert
        // under the other - which is the documented split rather than an oversight
        String gated = """
            { "conditions": [ { "input": { "kind": "WORLD", "world": "%s" }, "op": "IN", "keys": ["%s"] } ],
              "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 50 } } ] }
            """;

        ComputeFacts facts = new ComputeFacts("ISLAND", "BERSERK", "SPRING", 3, Concurrent.newMap());

        assertThat(world(String.format(gated, "MODE", "ISLAND"), facts), is(closeTo(50.0, EPSILON)));
        assertThat(world(String.format(gated, "DUNGEON_CLASS", "BERSERK"), facts), is(closeTo(50.0, EPSILON)));

        // wrong value, right property - the row applies and the condition says no
        assertThat(world(String.format(gated, "MODE", "IRONMAN"), facts), is(closeTo(0.0, EPSILON)));

        // a property a profiles response cannot carry, so nothing answers and the rule folds nothing
        assertThat(world(String.format(gated, "ZONE", "THE_GARDEN"), facts), is(closeTo(0.0, EPSILON)));
        assertThat(world(String.format(gated, "MAYOR", "TECHNOBLADE"), facts), is(closeTo(0.0, EPSILON)));
    }

    private static @NotNull ConcurrentMap<String, Integer> counted(int pieces) {
        return counted("YOUNG_DRAGON", pieces);
    }

    private static @NotNull ConcurrentMap<String, Integer> counted(@NotNull String group, int pieces) {
        ConcurrentMap<String, Integer> counts = Concurrent.newMap();
        counts.put(group, pieces);

        return counts;
    }

    private static double fold(@NotNull String row, @NotNull ConcurrentMap<String, Integer> counts) {
        return world(row, new ComputeFacts("", "", "", -1, counts));
    }

    private static double world(@NotNull String row, @NotNull ComputeFacts facts) {
        return BuffEvaluator.compile(GSON.fromJson(row, Buff.class)).apply(
            STRENGTH,
            Buff.Channel.VALUE,
            Buff.Rule.Stage.BONUS,
            0.0,
            facts.fill(BuffEvaluator.Context.of(Concurrent.newMap()))
        );
    }

}
