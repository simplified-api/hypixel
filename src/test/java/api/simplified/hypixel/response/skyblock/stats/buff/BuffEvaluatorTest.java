package api.simplified.hypixel.response.skyblock.stats.buff;

import api.simplified.skyblock.model.Buff;
import api.simplified.skyblock.model.Stat;
import com.google.gson.Gson;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.GsonSettings;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.IntTag;
import lib.minecraft.nbt.tag.StringTag;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

/**
 * Pins the buff grammar against hand-built rows.
 * <p>
 * The corpus cannot pin any of this: {@code buffs.json} ships no row and every legacy table it
 * replaces ships none either, which the characterisation harness asserts outright. Every case here is
 * therefore a row written by hand, one per grammar point, and this class is the only thing standing
 * behind the evaluator.
 * <p>
 * A row is built by binding JSON rather than by calling setters, which is the same path the corpus
 * takes - so a case here fails if the schema stops binding the shape it documents, and not only if
 * the arithmetic moves.
 */
class BuffEvaluatorTest {

    // the binder the corpus itself loads a row through - a bare Gson binds a mostly-empty tree
    private static final Gson GSON = GsonSettings.defaults().create();
    private static final double EPSILON = 1.0e-9;

    @Test
    @DisplayName("the operation is a column, and a plain add is the default")
    void theOperationIsAColumn() {
        Stat strength = stat("STRENGTH", true);

        assertThat(fold(row("""
            { "rules": [ { "target": { "kind": "STAT", "stat": "STRENGTH" },
                           "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """), strength, 100.0), is(closeTo(105.0, EPSILON)));

        assertThat(fold(row("""
            { "rules": [ { "operation": "SUBTRACT", "target": { "kind": "STAT", "stat": "STRENGTH" },
                           "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """), strength, 100.0), is(closeTo(95.0, EPSILON)));

        assertThat(fold(row("""
            { "rules": [ { "operation": "MULTIPLY", "target": { "kind": "STAT", "stat": "STRENGTH" },
                           "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """), strength, 100.0), is(closeTo(500.0, EPSILON)));

        assertThat(fold(row("""
            { "rules": [ { "operation": "DIVIDE", "target": { "kind": "STAT", "stat": "STRENGTH" },
                           "value": { "kind": "NUMBER", "amount": 4 } } ] }
            """), strength, 100.0), is(closeTo(25.0, EPSILON)));
    }

    @Test
    @DisplayName("a multiply of 0.5 halves the stat rather than adding half of it, and a percent unit says the other thing")
    void aFactorIsAFactorAndAPercentIsNot() {
        Stat strength = stat("STRENGTH", true);

        assertThat(fold(row("""
            { "rules": [ { "operation": "MULTIPLY", "target": { "kind": "ALL" },
                           "value": { "kind": "NUMBER", "amount": 0.5 } } ] }
            """), strength, 100.0), is(closeTo(50.0, EPSILON)));

        // the same number under the other unit is a raise of half again, which is the reading a unit
        // column exists to keep separable
        assertThat(fold(row("""
            { "rules": [ { "operation": "MULTIPLY", "unit": "PERCENT", "target": { "kind": "ALL" },
                           "value": { "kind": "NUMBER", "amount": 50 } } ] }
            """), strength, 100.0), is(closeTo(150.0, EPSILON)));
    }

    @Test
    @DisplayName("a scale is skipped for a stat that forbids one, and an add is not")
    void aScaleSkipsAStatThatForbidsOne() {
        Stat flat = stat("STRENGTH", false);

        assertThat(fold(row("""
            { "rules": [ { "operation": "MULTIPLY", "target": { "kind": "ALL" },
                           "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """), flat, 100.0), is(closeTo(100.0, EPSILON)));

        // dividing is multiplying by a reciprocal, so a guard that let it through would be a guard
        // with a documented way around it
        assertThat(fold(row("""
            { "rules": [ { "operation": "DIVIDE", "target": { "kind": "ALL" },
                           "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """), flat, 100.0), is(closeTo(100.0, EPSILON)));

        assertThat(fold(row("""
            { "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """), flat, 100.0), is(closeTo(105.0, EPSILON)));
    }

    @Test
    @DisplayName("rules fold in operation order rather than in the order the file lists them")
    void rulesFoldInOperationOrder() {
        Stat strength = stat("STRENGTH", true);

        // the multiply is written first and folds second, so it sees the finished additive sum
        String scaleFirst = """
            { "rules": [ { "operation": "MULTIPLY", "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 2 } },
                         { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 10 } } ] }
            """;
        String addFirst = """
            { "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 10 } },
                         { "operation": "MULTIPLY", "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 2 } } ] }
            """;

        assertThat(fold(row(scaleFirst), strength, 100.0), is(closeTo(220.0, EPSILON)));
        assertThat(fold(row(addFirst), strength, 100.0), is(closeTo(220.0, EPSILON)));

        // and the reading it is not: 100 doubled and then 10 added would be 210
        assertThat(fold(row(scaleFirst), strength, 100.0), is(closeTo(fold(row(addFirst), strength, 100.0), EPSILON)));
    }

    @Test
    @DisplayName("the effects shorthand folds as the add it stands for")
    void theEffectsShorthandFoldsAsAnAdd() {
        // 100 plus 10 flat, then doubled - not 100 doubled and then 10 added
        assertThat(fold(row("""
            { "effects": { "STRENGTH": 10 },
              "rules": [ { "operation": "MULTIPLY", "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 2 } } ] }
            """), stat("STRENGTH", true), 100.0), is(closeTo(220.0, EPSILON)));
    }

    @Test
    @DisplayName("a target names one stat, every stat, or every stat but the ones it excepts")
    void aTargetNamesWhichStatsAreWritten() {
        Stat strength = stat("STRENGTH", true);
        Stat health = stat("HEALTH", true);

        String one = """
            { "rules": [ { "target": { "kind": "STAT", "stat": "STRENGTH" }, "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """;
        String all = """
            { "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """;
        String allBut = """
            { "rules": [ { "target": { "kind": "ALL", "except": ["HEALTH"] }, "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """;

        assertThat(fold(row(one), strength, 100.0), is(closeTo(105.0, EPSILON)));
        assertThat(fold(row(one), health, 100.0), is(closeTo(100.0, EPSILON)));
        assertThat(fold(row(all), health, 100.0), is(closeTo(105.0, EPSILON)));
        assertThat(fold(row(allBut), strength, 100.0), is(closeTo(105.0, EPSILON)));
        assertThat(fold(row(allBut), health, 100.0), is(closeTo(100.0, EPSILON)));
    }

    @Test
    @DisplayName("a stat term of TARGET reads whichever stat is being written")
    void aStatTermOfTargetReadsTheStatBeingWritten() {
        ConcurrentMap<String, Double> variables = Concurrent.newMap();
        variables.put("STAT_ACTIVE_PET_STRENGTH", 30.0);
        variables.put("STAT_ACTIVE_PET_HEALTH", 70.0);

        // one row, one compile, a different number per stat - which is what the spelling it replaces
        // needed a second every-stat constant to say
        Buff row = row("""
            { "rules": [ { "target": { "kind": "ALL" },
                           "value": { "kind": "STAT", "stat": "TARGET", "origin": "ACTIVE_PET" } } ] }
            """);

        assertThat(fold(row, stat("STRENGTH", true), 100.0, variables), is(closeTo(130.0, EPSILON)));
        assertThat(fold(row, stat("HEALTH", true), 100.0, variables), is(closeTo(170.0, EPSILON)));

        // a stat the origin never wrote is absent rather than zero, so the rule does not fold at all
        assertThat(fold(row, stat("DEFENSE", true), 100.0, variables), is(closeTo(100.0, EPSILON)));
    }

    @Test
    @DisplayName("a condition gates the row, and a rule's own condition gates only that rule")
    void conditionsGateTheRowAndTheRule() {
        Stat strength = stat("STRENGTH", true);

        String gatedRow = """
            { "conditions": [ { "input": { "kind": "COUNT", "group": "YOUNG_DRAGON" }, "op": "GTE", "amount": 4 } ],
              "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """;

        assertThat(fold(row(gatedRow), strength, 100.0, Concurrent.newMap(), context -> context.groupCount("YOUNG_DRAGON", 4)), is(closeTo(105.0, EPSILON)));
        assertThat(fold(row(gatedRow), strength, 100.0, Concurrent.newMap(), context -> context.groupCount("YOUNG_DRAGON", 3)), is(closeTo(100.0, EPSILON)));

        // a group this context has no count for is a reading it cannot make, so the row stays inert
        assertThat(fold(row(gatedRow), strength, 100.0), is(closeTo(100.0, EPSILON)));
    }

    @Test
    @DisplayName("a carrier term reads a key for membership and a number for arithmetic")
    void aCarrierTermReadsAKeyOrANumber() {
        Stat strength = stat("STRENGTH", true);

        String gatedOnRarity = """
            { "conditions": [ { "input": { "kind": "CARRIER", "carrier": "RARITY" }, "op": "IN", "keys": ["LEGENDARY", "MYTHIC"] } ],
              "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "CARRIER", "carrier": "LEVEL" } } ] }
            """;

        assertThat(
            fold(row(gatedOnRarity), strength, 100.0, Concurrent.newMap(), context -> context
                .carrier(Buff.Term.Carrier.RARITY, "LEGENDARY")
                .carrier(Buff.Term.Carrier.LEVEL, 25.0)),
            is(closeTo(125.0, EPSILON))
        );

        assertThat(
            fold(row(gatedOnRarity), strength, 100.0, Concurrent.newMap(), context -> context
                .carrier(Buff.Term.Carrier.RARITY, "RARE")
                .carrier(Buff.Term.Carrier.LEVEL, 25.0)),
            is(closeTo(100.0, EPSILON))
        );
    }

    @Test
    @DisplayName("a tag term reads a declared path, and tells an absent tag from a zero")
    void aTagTermReadsADeclaredPath() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putPath("tag.ExtraAttributes.first", new IntTag(7));
        compoundTag.putPath("tag.ExtraAttributes.name", new StringTag("SNAIL"));

        Buff numeric = row("""
            { "rules": [ { "target": { "kind": "ALL" },
                           "value": { "kind": "TAG", "path": "tag.ExtraAttributes.first", "read": "NUMBER" } } ] }
            """);

        assertThat(fold(numeric, stat("STRENGTH", true), 100.0, Concurrent.newMap(), context -> context.tag(compoundTag)), is(closeTo(107.0, EPSILON)));

        // the path this replaces was spliced into an expression and could only ever come back a
        // number, so an absent path read as a zero and contributed one
        assertThat(fold(row("""
            { "rules": [ { "target": { "kind": "ALL" },
                           "value": { "kind": "TAG", "path": "tag.ExtraAttributes.missing", "read": "NUMBER" } } ] }
            """), stat("STRENGTH", true), 100.0, Concurrent.newMap(), context -> context.tag(compoundTag)), is(closeTo(100.0, EPSILON)));

        // and a text reading is a key, which is what an equality against a name compares
        assertThat(fold(row("""
            { "conditions": [ { "input": { "kind": "TAG", "path": "tag.ExtraAttributes.name", "read": "TEXT" }, "op": "EQ", "key": "SNAIL" } ],
              "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """), stat("STRENGTH", true), 100.0, Concurrent.newMap(), context -> context.tag(compoundTag)), is(closeTo(105.0, EPSILON)));
    }

    @Test
    @DisplayName("a threshold ladder reads the greatest rung reached, and a cumulative one sums every rung")
    void aLadderReadsItsRungs() {
        Stat strength = stat("STRENGTH", true);
        ConcurrentMap<String, Double> variables = Concurrent.newMap();
        variables.put("SKILL_AVERAGE", 17.0);

        String ladder = """
            { "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "LOOKUP", "lookup": {
                "on": { "kind": "VARIABLE", "name": "SKILL_AVERAGE" },
                "mode": "%s",
                "amounts": { "1": 5, "15": 3, "20": 4 } } } } ] }
            """;

        assertThat(fold(row(String.format(ladder, "THRESHOLD")), strength, 100.0, variables), is(closeTo(103.0, EPSILON)));
        assertThat(fold(row(String.format(ladder, "THRESHOLD_CUMULATIVE")), strength, 100.0, variables), is(closeTo(108.0, EPSILON)));

        // an exact table answers nothing off a rung, which leaves the rule unevaluated
        assertThat(fold(row(String.format(ladder, "EXACT")), strength, 100.0, variables), is(closeTo(100.0, EPSILON)));
    }

    @Test
    @DisplayName("a rung is reached at equality rather than past it")
    void aRungIsReachedAtEquality() {
        Stat strength = stat("STRENGTH", true);
        ConcurrentMap<String, Double> variables = Concurrent.newMap();

        String ladder = """
            { "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "LOOKUP", "lookup": {
                "on": { "kind": "VARIABLE", "name": "SKILL_AVERAGE" },
                "mode": "THRESHOLD_CUMULATIVE",
                "amounts": { "1": 2, "15": 1 } } } } ] }
            """;

        // at 15 exactly both rungs count; one band lower only the first does, which is the whole of
        // what the two readings differ by
        variables.put("SKILL_AVERAGE", 15.0);
        assertThat(fold(row(ladder), strength, 100.0, variables), is(closeTo(103.0, EPSILON)));

        variables.put("SKILL_AVERAGE", 14.0);
        assertThat(fold(row(ladder), strength, 100.0, variables), is(closeTo(102.0, EPSILON)));
    }

    @Test
    @DisplayName("an expression names only inputs it declares, and one it cannot read leaves it unevaluated")
    void anExpressionNamesOnlyDeclaredInputs() {
        Stat strength = stat("STRENGTH", true);

        assertThat(fold(row("""
            { "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "MATH", "text": "base + step * level", "inputs": {
                "base": { "kind": "NUMBER", "amount": 5 },
                "step": { "kind": "NUMBER", "amount": 2 },
                "level": { "kind": "CARRIER", "carrier": "LEVEL" } } } } ] }
            """), strength, 100.0, Concurrent.newMap(), context -> context.carrier(Buff.Term.Carrier.LEVEL, 10.0)), is(closeTo(125.0, EPSILON)));

        // the level is undeclarable here, so the expression is unevaluable and the rule folds nothing
        assertThat(fold(row("""
            { "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "MATH", "text": "base + level", "inputs": {
                "base": { "kind": "NUMBER", "amount": 5 },
                "level": { "kind": "PLAYER", "player": "LIGHT_LEVEL" } } } } ] }
            """), strength, 100.0), is(closeTo(100.0, EPSILON)));
    }

    @Test
    @DisplayName("a term nothing supplies leaves the rule inert rather than folding a zero")
    void anUnreadableTermLeavesTheRuleInert() {
        Stat strength = stat("STRENGTH", true);

        // a zero operand under an add would be invisible, so the case that proves it is a multiply -
        // folding an unread term as zero would wipe the number out
        assertThat(fold(row("""
            { "rules": [ { "operation": "MULTIPLY", "target": { "kind": "ALL" },
                           "value": { "kind": "PLAYER", "player": "HEALTH_FRACTION" } } ] }
            """), strength, 100.0), is(closeTo(100.0, EPSILON)));

        assertThat(fold(row("""
            { "rules": [ { "operation": "MULTIPLY", "target": { "kind": "ALL" },
                           "value": { "kind": "WORLD", "world": "REGION" } } ] }
            """), strength, 100.0), is(closeTo(100.0, EPSILON)));
    }

    @Test
    @DisplayName("a ceiling and a floor bound one rule's own contribution")
    void aCeilingAndAFloorBoundOneRule() {
        Stat strength = stat("STRENGTH", true);

        assertThat(fold(row("""
            { "rules": [ { "target": { "kind": "ALL" }, "ceiling": 3, "value": { "kind": "NUMBER", "amount": 50 } } ] }
            """), strength, 100.0), is(closeTo(103.0, EPSILON)));

        assertThat(fold(row("""
            { "rules": [ { "operation": "SUBTRACT", "target": { "kind": "ALL" }, "floor": 2,
                           "value": { "kind": "NUMBER", "amount": -50 } } ] }
            """), strength, 100.0), is(closeTo(98.0, EPSILON)));
    }

    @Test
    @DisplayName("a rule declares its stage, and a rarity rule takes one from the channel it writes")
    void aRuleDeclaresOrDerivesItsStage() {
        Stat strength = stat("STRENGTH", true);

        Buff post = row("""
            { "rules": [ { "stage": "POST", "operation": "MULTIPLY", "target": { "kind": "ALL" },
                           "value": { "kind": "NUMBER", "amount": 2 } } ] }
            """);

        assertThat(fold(post, strength, 100.0), is(closeTo(100.0, EPSILON)));
        assertThat(
            BuffEvaluator.compile(post).apply(strength, Buff.Channel.VALUE, Buff.Rule.Stage.POST, 100.0, BuffEvaluator.Context.of(Concurrent.newMap())),
            is(closeTo(200.0, EPSILON))
        );

        // a rule writing nothing but a rarity resolves before any total exists, so stating its stage
        // would be stating what its channel already says
        Buff rarity = row("""
            { "rules": [ { "channels": ["RARITY"], "value": { "kind": "NUMBER", "amount": 1 } } ] }
            """);

        assertThat(rarity.getRules().getFirst().getStage(), is(Buff.Rule.Stage.RARITY));
        assertThat(
            BuffEvaluator.compile(rarity).applyRarity(4.0, BuffEvaluator.Context.of(Concurrent.newMap())),
            is(closeTo(5.0, EPSILON))
        );

        // and it is unreachable through the stat path, because a rule with no target matches no stat
        assertThat(
            BuffEvaluator.compile(rarity).apply(strength, Buff.Channel.RARITY, Buff.Rule.Stage.RARITY, 4.0, BuffEvaluator.Context.of(Concurrent.newMap())),
            is(closeTo(4.0, EPSILON))
        );
    }

    @Test
    @DisplayName("a channel decides which number a rule reaches")
    void aChannelDecidesWhichNumberIsWritten() {
        Stat strength = stat("STRENGTH", true);

        // one operand, both numbers - which is what saves the day a raise changes from being written
        // twice and only one copy moving
        Buff both = row("""
            { "rules": [ { "channels": ["VALUE", "CAP"], "target": { "kind": "ALL" },
                           "value": { "kind": "NUMBER", "amount": 100 } } ] }
            """);

        BuffEvaluator evaluator = BuffEvaluator.compile(both);
        BuffEvaluator.Context context = BuffEvaluator.Context.of(Concurrent.newMap());

        assertThat(evaluator.apply(strength, Buff.Channel.VALUE, Buff.Rule.Stage.BONUS, 300.0, context), is(closeTo(400.0, EPSILON)));
        assertThat(evaluator.apply(strength, Buff.Channel.CAP, Buff.Rule.Stage.BONUS, 400.0, context), is(closeTo(500.0, EPSILON)));

        // and a rule writing only the value leaves the ceiling alone
        Buff valueOnly = row("""
            { "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 100 } } ] }
            """);

        assertThat(
            BuffEvaluator.compile(valueOnly).apply(strength, Buff.Channel.CAP, Buff.Rule.Stage.BONUS, 400.0, context),
            is(closeTo(400.0, EPSILON))
        );
    }

    @Test
    @DisplayName("a diurnal window is a condition over ranges, and it wraps past midnight")
    void aDiurnalWindowIsAConditionOverRanges() {
        Stat strength = stat("STRENGTH", true);

        // the spelling this replaces could not say a window that wraps at all, so a night range had
        // to be written as a pair the parser could not read back
        String night = """
            { "conditions": [ { "input": { "kind": "WORLD", "world": "HOUR" }, "op": "WITHIN",
                                "ranges": [ { "low": 20, "high": 23 }, { "low": 0, "high": 5 } ] } ],
              "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 5 } } ] }
            """;

        assertThat(fold(row(night), strength, 100.0, Concurrent.newMap(), context -> context.world(Buff.Term.World.HOUR, 2.0)), is(closeTo(105.0, EPSILON)));
        assertThat(fold(row(night), strength, 100.0, Concurrent.newMap(), context -> context.world(Buff.Term.World.HOUR, 22.0)), is(closeTo(105.0, EPSILON)));
        assertThat(fold(row(night), strength, 100.0, Concurrent.newMap(), context -> context.world(Buff.Term.World.HOUR, 12.0)), is(closeTo(100.0, EPSILON)));

        // an hour nothing supplies leaves the row alone rather than zeroing what it guards, which is
        // the whole of what changed about a time gate
        assertThat(fold(row(night), strength, 100.0), is(closeTo(100.0, EPSILON)));
    }

    private static double fold(@NotNull Buff row, @NotNull Stat statModel, double current) {
        return fold(row, statModel, current, Concurrent.newMap());
    }

    private static double fold(@NotNull Buff row, @NotNull Stat statModel, double current, @NotNull ConcurrentMap<String, Double> variables) {
        return fold(row, statModel, current, variables, context -> context);
    }

    private static double fold(
        @NotNull Buff row,
        @NotNull Stat statModel,
        double current,
        @NotNull ConcurrentMap<String, Double> variables,
        @NotNull UnaryOperator<BuffEvaluator.Context> fill
    ) {
        return BuffEvaluator.compile(row)
            .apply(statModel, Buff.Channel.VALUE, Buff.Rule.Stage.BONUS, current, fill.apply(BuffEvaluator.Context.of(variables)));
    }

    private static @NotNull Stat stat(@NotNull String id, boolean multiplicable) {
        return GSON.fromJson(String.format("{\"id\":\"%s\",\"multiplicable\":%s}", id, multiplicable), Stat.class);
    }

    private static @NotNull Buff row(@NotNull String json) {
        return GSON.fromJson(json, Buff.class);
    }

}
