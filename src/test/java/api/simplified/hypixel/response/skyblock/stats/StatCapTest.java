package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.stats.buff.BuffEvaluator;
import api.simplified.skyblock.model.Buff;
import api.simplified.skyblock.model.Stat;
import com.google.gson.Gson;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.gson.GsonSettings;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

/**
 * Pins how a ceiling resolves, which nothing on the fixture exercises.
 * <p>
 * Of the eight cap shapes the design records, seven name an item or a pet item nobody on the capture
 * carries and the eighth names a mayor the corpus does not have - so every resolved cap on the fixture
 * equals its own column and the reduction is reached only from here. That makes this the whole of the
 * evidence for it rather than a supplement to a golden number.
 * <p>
 * Nothing here opens a session. A cap resolves from a stat, a list of evaluators and their contexts,
 * which is the entire input.
 */
class StatCapTest {

    private static final Gson GSON = GsonSettings.defaults().create();
    private static final double EPSILON = 1.0E-6;

    /**
     * The corpus gives Speed a ceiling of 400, which is the cap every recorded raise is written
     * against.
     */
    private static final @NotNull Stat WALK_SPEED = GSON.fromJson("{\"id\":\"WALK_SPEED\",\"multiplicable\":true,\"cap\":400}", Stat.class);

    private static final @NotNull Stat ATTACK_SPEED = GSON.fromJson("{\"id\":\"ATTACK_SPEED\",\"multiplicable\":true,\"cap\":100}", Stat.class);

    /**
     * Crit Chance reads 152 percent in the stat menu against no cap column at all, so it is the case
     * that says an absent ceiling is uncapped rather than a ceiling of zero.
     */
    private static final @NotNull Stat CRIT_CHANCE = GSON.fromJson("{\"id\":\"CRIT_CHANCE\",\"multiplicable\":true}", Stat.class);

    @Test
    @DisplayName("a stat's own column is the ceiling when nothing writes the cap channel")
    void aStatsOwnColumnIsTheCeiling() {
        assertThat(resolve(WALK_SPEED).ceiling().orElseThrow(), is(closeTo(400.0, EPSILON)));

        // an absent column is uncapped rather than a ceiling of zero, which is the whole reason the
        // ceiling is optional instead of a number
        assertThat(resolve(CRIT_CHANCE).isUncapped(), is(true));
        assertThat(resolve(CRIT_CHANCE).clamp(152.0), is(closeTo(152.0, EPSILON)));
    }

    @Test
    @DisplayName("two raises stack, because raises are additive")
    void twoRaisesStack() {
        assertThat(resolve(WALK_SPEED, raise(100)).ceiling().orElseThrow(), is(closeTo(500.0, EPSILON)));

        // 400 + 100 + 100, which is the number read in game off Young Dragon with a Black Cat
        // equipped - additive rather than highest-wins, and this is where that reading is recorded
        assertThat(resolve(WALK_SPEED, raise(100), raise(100)).ceiling().orElseThrow(), is(closeTo(600.0, EPSILON)));
    }

    @Test
    @DisplayName("a gated raise contributes only where its condition holds")
    void aGatedRaiseContributesOnlyWhereItHolds() {
        String gated = """
            { "conditions": [ { "input": { "kind": "CARRIER", "carrier": "RARITY" }, "op": "IN", "keys": ["LEGENDARY"] } ],
              "rules": [ { "channels": ["CAP"], "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 100 } } ] }
            """;

        assertThat(
            StatCap.resolve(WALK_SPEED, scoped(gated, context -> context.carrier(Buff.Term.Carrier.RARITY, "LEGENDARY"))).ceiling().orElseThrow(),
            is(closeTo(500.0, EPSILON))
        );

        // the same row on a carrier it does not apply to leaves the column's ceiling alone rather
        // than raising it to some default
        assertThat(
            StatCap.resolve(WALK_SPEED, scoped(gated, context -> context.carrier(Buff.Term.Carrier.RARITY, "RARE"))).ceiling().orElseThrow(),
            is(closeTo(400.0, EPSILON))
        );
    }

    @Test
    @DisplayName("a set replaces every raise before it, and a raise on one stat leaves another alone")
    void aSetReplacesEveryRaiseBeforeIt() {
        // assignments are rank three and raises are rank one, so the set folds last whatever order
        // the rules are spelled in - a replacement rather than a downward clamp on what was raised
        String raiseThenSet = """
            { "rules": [ { "channels": ["CAP"], "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 100 } },
                         { "channels": ["CAP"], "operation": "SET", "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 250 } } ] }
            """;

        assertThat(resolve(WALK_SPEED, raiseThenSet).ceiling().orElseThrow(), is(closeTo(250.0, EPSILON)));

        // a set is also the only thing that gives an uncapped stat a ceiling - a raise on one has
        // nothing to be relative to and leaves it uncapped
        assertThat(resolve(CRIT_CHANCE, raise(100)).isUncapped(), is(true));
        assertThat(resolve(CRIT_CHANCE, raiseThenSet).ceiling().orElseThrow(), is(closeTo(250.0, EPSILON)));

        // a rule naming one stat does not reach another, so two capped stats resolve independently
        String speedOnly = """
            { "rules": [ { "channels": ["CAP"], "target": { "kind": "STAT", "stat": "WALK_SPEED" }, "value": { "kind": "NUMBER", "amount": 100 } } ] }
            """;

        assertThat(resolve(WALK_SPEED, speedOnly).ceiling().orElseThrow(), is(closeTo(500.0, EPSILON)));
        assertThat(resolve(ATTACK_SPEED, speedOnly).ceiling().orElseThrow(), is(closeTo(100.0, EPSILON)));
    }

    @Test
    @DisplayName("a removal takes the ceiling away rather than setting it to a number")
    void aRemovalTakesTheCeilingAway() {
        String removes = """
            { "rules": [ { "channels": ["CAP"], "operation": "REMOVE", "target": { "kind": "ALL" } } ] }
            """;

        assertThat(resolve(WALK_SPEED, removes).isUncapped(), is(true));

        // and it wins over a raise in the same program whichever way round they are read, because a
        // removal is answered before any arithmetic runs rather than by one
        assertThat(resolve(WALK_SPEED, raise(100), removes).isUncapped(), is(true));
        assertThat(resolve(WALK_SPEED, removes, raise(100)).isUncapped(), is(true));
    }

    @Test
    @DisplayName("a clamp reads a total down to the ceiling and never up to it")
    void aClampReadsATotalDownToTheCeiling() {
        StatCap capped = resolve(WALK_SPEED);

        assertThat(capped.clamp(500.0), is(closeTo(400.0, EPSILON)));
        assertThat(capped.clamp(374.0), is(closeTo(374.0, EPSILON)));

        // exactly at the ceiling is not over it
        assertThat(capped.clamp(400.0), is(closeTo(400.0, EPSILON)));

        // the fixture's Speed reads 374 against 400, and the eighteen active century cakes are worth
        // at most 10 each - so nothing on it clamps, which is why no golden cell moves
        assertThat(capped.clamp(384.0), is(closeTo(384.0, EPSILON)));
    }

    private static @NotNull String raise(int amount) {
        return String.format("{ \"rules\": [ { \"channels\": [\"CAP\"], \"target\": { \"kind\": \"ALL\" }, \"value\": { \"kind\": \"NUMBER\", \"amount\": %d } } ] }", amount);
    }

    private static @NotNull StatCap resolve(@NotNull Stat statModel, @NotNull String... rows) {
        ConcurrentList<StatCap.Scoped> program = Concurrent.newList();

        for (String row : rows)
            program.addAll(scoped(row, context -> context));

        return StatCap.resolve(statModel, program);
    }

    private static @NotNull ConcurrentList<StatCap.Scoped> scoped(@NotNull String row, @NotNull UnaryOperator<BuffEvaluator.Context> fill) {
        return Concurrent.newList(new StatCap.Scoped(
            BuffEvaluator.compile(GSON.fromJson(row, Buff.class)),
            fill.apply(BuffEvaluator.Context.of(Concurrent.newMap()))
        ));
    }

}
