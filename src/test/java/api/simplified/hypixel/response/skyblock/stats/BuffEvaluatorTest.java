package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.skyblock.model.BuffEffectsModel;
import api.simplified.skyblock.model.Stat;
import com.google.gson.Gson;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.GsonSettings;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.IntTag;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

/**
 * Pins the buff-effect grammar against hand-built rows.
 * <p>
 * The corpus cannot pin any of this: all six tables that would carry a row ship none, which the
 * characterisation harness asserts outright. Every case here is therefore a row written by hand, one
 * per grammar point, and this class is the only thing standing behind the evaluator.
 */
class BuffEvaluatorTest {

    // the binder the corpus itself loads a Stat through - a bare Gson cannot, the row declares two
    // fields against the one `category` key
    private static final Gson GSON = GsonSettings.defaults().create();
    private static final double EPSILON = 1.0e-9;

    @Test
    @DisplayName("the prefix decides the operation, and a plain number is an operand like any other")
    void thePrefixDecidesTheOperation() {
        Stat strength = stat("STRENGTH", true);
        ConcurrentMap<String, Double> variables = Concurrent.newMap();

        // the shape this replaces multiplied by a constant whatever the prefix said, so ADD_ meant "times five"
        assertThat(evaluate(row(Map.of("ADD_STRENGTH", 5.0)), strength, 100.0, variables), is(closeTo(105.0, EPSILON)));
        assertThat(evaluate(row(Map.of("STRENGTH", 5.0)), strength, 100.0, variables), is(closeTo(105.0, EPSILON)));
        assertThat(evaluate(row(Map.of("MULTIPLY_STRENGTH", 5.0)), strength, 100.0, variables), is(closeTo(500.0, EPSILON)));
    }

    @Test
    @DisplayName("MULTIPLY_ALL 0.5 halves the stat rather than adding half of it")
    void multiplyAllIsAFactorRatherThanAFraction() {
        assertThat(
            evaluate(row(Map.of("MULTIPLY_ALL", 0.5)), stat("STRENGTH", true), 100.0, Concurrent.newMap()),
            is(closeTo(50.0, EPSILON))
        );
    }

    @Test
    @DisplayName("a multiply is skipped for a stat that cannot be multiplied, and an add is not")
    void multiplySkipsAStatThatCannotBeMultiplied() {
        Stat flat = stat("STRENGTH", false);
        ConcurrentMap<String, Double> variables = Concurrent.newMap();

        assertThat(evaluate(row(Map.of("MULTIPLY_STRENGTH", 5.0)), flat, 100.0, variables), is(closeTo(100.0, EPSILON)));
        assertThat(evaluate(row(Map.of("ADD_STRENGTH", 5.0)), flat, 100.0, variables), is(closeTo(105.0, EPSILON)));
    }

    @Test
    @DisplayName("ALL and PET_ALL match every stat, and a stat id matches only itself")
    void everyStatSpellingMatchesEveryStat() {
        Stat strength = stat("STRENGTH", true);
        Stat health = stat("HEALTH", true);
        ConcurrentMap<String, Double> variables = Concurrent.newMap();

        assertThat(evaluate(row(Map.of("ADD_ALL", 5.0)), health, 100.0, variables), is(closeTo(105.0, EPSILON)));
        assertThat(evaluate(row(Map.of("ADD_PET_ALL", 5.0)), health, 100.0, variables), is(closeTo(105.0, EPSILON)));
        assertThat(evaluate(row(Map.of("ADD_STRENGTH", 5.0)), health, 100.0, variables), is(closeTo(100.0, EPSILON)));
        assertThat(evaluate(row(Map.of("ADD_STRENGTH", 5.0)), strength, 100.0, variables), is(closeTo(105.0, EPSILON)));
    }

    @Test
    @DisplayName("a TIME guard outside every hour range zeroes the value rather than skipping it")
    void aTimeGuardOutsideEveryRangeZeroes() {
        Stat strength = stat("STRENGTH", true);
        ConcurrentMap<String, Double> variables = Concurrent.newMap();

        // a SkyBlock hour is 0 to 23, so a range past the end covers no hour whenever this runs
        assertThat(
            evaluate(row(Map.of("TIME", List.of("30-31"), "ADD_STRENGTH", 5.0)), strength, 100.0, variables),
            is(closeTo(0.0, EPSILON))
        );
        assertThat(
            evaluate(row(Map.of("TIME", List.of("0-23"), "ADD_STRENGTH", 5.0)), strength, 100.0, variables),
            is(closeTo(105.0, EPSILON))
        );
    }

    @Test
    @DisplayName("an hour range is inclusive at both ends")
    void anHourRangeIsInclusiveAtBothEnds() {
        HourRange range = HourRange.parse("19-23");

        assertThat(range.start(), is(19));
        assertThat(range.end(), is(23));
        assertThat(range.contains(19), is(true));
        assertThat(range.contains(23), is(true));
        assertThat(range.contains(18), is(false));
        assertThat(range.contains(0), is(false));
    }

    @Test
    @DisplayName("a value naming two nbt paths substitutes both")
    void aValueNamingTwoNbtPathsSubstitutesBoth() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putPath("tag.ExtraAttributes.first", new IntTag(7));
        compoundTag.putPath("tag.ExtraAttributes.second", new IntTag(11));

        // the shape this replaces substituted group one only, so the second reference reached the parser
        double result = evaluate(
            row(Map.of("ADD_STRENGTH", "nbt_tag.ExtraAttributes.first + nbt_tag.ExtraAttributes.second")),
            stat("STRENGTH", true),
            100.0,
            Concurrent.newMap(),
            compoundTag
        );

        assertThat(result, is(closeTo(118.0, EPSILON)));
    }

    @Test
    @DisplayName("CURRENT_VALUE names the running total instead of throwing at build")
    void currentValueNamesTheRunningTotal() {
        assertThat(
            evaluate(row(Map.of("ADD_STRENGTH", "CURRENT_VALUE * 2")), stat("STRENGTH", true), 50.0, Concurrent.newMap()),
            is(closeTo(150.0, EPSILON))
        );
    }

    @Test
    @DisplayName("one PET_ALL text binds per stat, so a single compile serves every stat")
    void petAllBindsPerStat() {
        ConcurrentMap<String, Double> variables = Concurrent.newMap();
        variables.put("STAT_PET_STRENGTH", 30.0);
        variables.put("STAT_PET_HEALTH", 70.0);

        BuffEvaluator evaluator = BuffEvaluator.compile(row(Map.of("ADD_ALL", "PET_ALL")));

        assertThat(evaluator.apply(stat("STRENGTH", true), 100.0, variables, Operation.Pass.BONUS), is(closeTo(130.0, EPSILON)));
        assertThat(evaluator.apply(stat("HEALTH", true), 100.0, variables, Operation.Pass.BONUS), is(closeTo(170.0, EPSILON)));

        // a stat the pet gives nothing for reads zero rather than leaving the name unbound
        assertThat(evaluator.apply(stat("DEFENSE", true), 100.0, variables, Operation.Pass.BONUS), is(closeTo(100.0, EPSILON)));
    }

    @Test
    @DisplayName("the effects map is added before any rule runs")
    void theEffectsMapIsAddedFirst() {
        BuffEffectsModel model = row(Map.of("STRENGTH", 10.0), Map.of("MULTIPLY_STRENGTH", 2.0));

        // 100 + 10 flat, then doubled - not 100 doubled and then 10 added
        assertThat(
            evaluate(model, stat("STRENGTH", true), 100.0, Concurrent.newMap()),
            is(closeTo(220.0, EPSILON))
        );
    }

    @Test
    @DisplayName("COPY and SET belong to the post pass and are inert during the bonus pass")
    void postPassOperationsAreInertDuringTheBonusPass() {
        Stat strength = stat("STRENGTH", true);
        ConcurrentMap<String, Double> variables = Concurrent.newMap();

        assertThat(evaluate(row(Map.of("COPY_STRENGTH", 2.0)), strength, 100.0, variables), is(closeTo(100.0, EPSILON)));
        assertThat(evaluate(row(Map.of("SET_STRENGTH", 2.0)), strength, 100.0, variables), is(closeTo(100.0, EPSILON)));

        BuffEvaluator evaluator = BuffEvaluator.compile(row(Map.of("COPY_STRENGTH", 2.0)));
        assertThat(evaluator.apply(strength, 100.0, variables, Operation.Pass.POST), is(closeTo(200.0, EPSILON)));

        BuffEvaluator setter = BuffEvaluator.compile(row(Map.of("SET_STRENGTH", 2.0)));
        assertThat(setter.apply(strength, 100.0, variables, Operation.Pass.POST), is(closeTo(2.0, EPSILON)));
    }

    @Test
    @DisplayName("a key names its operation by prefix, and an unprefixed key is a bare stat id")
    void aKeyNamesItsOperationByPrefix() {
        assertThat(Operation.of("ADD_STRENGTH"), is(Operation.ADD));
        assertThat(Operation.of("MULTIPLY_ALL"), is(Operation.MULTIPLY));
        assertThat(Operation.of("COPY_PET_ALL"), is(Operation.COPY));
        assertThat(Operation.of("SET_CRIT_CHANCE"), is(Operation.SET));
        assertThat(Operation.of("STRENGTH"), is(Operation.NONE));

        assertThat(BuffRule.parse("MULTIPLY_ALL", 0.5).getTarget(), is("ALL"));
        assertThat(BuffRule.parse("MULTIPLY_ALL", 0.5).isEveryStat(), is(true));
        assertThat(BuffRule.parse("ADD_STRENGTH", 5.0).getTarget(), is("STRENGTH"));
        assertThat(BuffRule.parse("ADD_STRENGTH", 5.0).isEveryStat(), is(false));
        assertThat(BuffRule.parse("ADD_STRENGTH", 5.0).getConstant(), is(5.0));
        assertThat(BuffRule.parse("ADD_STRENGTH", "CURRENT_VALUE").getConstant(), is((Double) null));
        assertThat(BuffRule.parse("ADD_STRENGTH", "CURRENT_VALUE").getExpressionText(), is("CURRENT_VALUE"));
    }

    private static double evaluate(@NotNull BuffEffectsModel model, @NotNull Stat statModel, double current, @NotNull ConcurrentMap<String, Double> variables) {
        return evaluate(model, statModel, current, variables, null);
    }

    private static double evaluate(@NotNull BuffEffectsModel model, @NotNull Stat statModel, double current, @NotNull ConcurrentMap<String, Double> variables, CompoundTag compoundTag) {
        return BuffEvaluator.compile(model)
            .bind(compoundTag)
            .apply(statModel, current, variables, Operation.Pass.BONUS);
    }

    private static @NotNull Stat stat(@NotNull String id, boolean multiplicable) {
        return GSON.fromJson(String.format("{\"id\":\"%s\",\"multiplicable\":%s}", id, multiplicable), Stat.class);
    }

    private static @NotNull BuffEffectsModel row(@NotNull Map<String, Object> buffEffects) {
        return row(Map.of(), buffEffects);
    }

    private static @NotNull BuffEffectsModel row(@NotNull Map<String, Double> effects, @NotNull Map<String, Object> buffEffects) {
        return new BuffEffectsModel() {

            @Override
            public @NotNull Map<String, Double> getEffects() {
                return effects;
            }

            @Override
            public @NotNull Map<String, Object> getBuffEffects() {
                return buffEffects;
            }

        };
    }

}
