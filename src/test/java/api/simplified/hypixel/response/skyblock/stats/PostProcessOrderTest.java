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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

/**
 * Pins what the post process's declared sequence is for: that running it in a different order
 * produces a different number, and that the rounds sit in the order the totals they read require.
 * <p>
 * The golden file cannot see any of this. It records one run in one order, and three of the rounds
 * need a row in a table that ships none - so the sequence is driven here from a given order, against
 * rows built in the test rather than read from a corpus.
 * <p>
 * Nothing here opens a session, reads the fixture or touches the corpus. A profile cell is written
 * through the sink and the program is handed in, which is the whole reason the pass takes one.
 */
class PostProcessOrderTest {

    private static final Gson GSON = GsonSettings.defaults().create();
    private static final double EPSILON = 1.0E-6;

    private static final @NotNull Stat STRENGTH = GSON.fromJson("{\"id\":\"STRENGTH\",\"multiplicable\":true}", Stat.class);

    private static final @NotNull String ADD_FIFTY = """
        { "rules": [ { "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 50 } } ] }
        """;

    private static final @NotNull String DOUBLE_ON_BONUS = """
        { "rules": [ { "operation": "MULTIPLY", "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 2 } } ] }
        """;

    private static final @NotNull String DOUBLE_ON_POST = """
        { "rules": [ { "stage": "POST", "operation": "MULTIPLY", "target": { "kind": "ALL" }, "value": { "kind": "NUMBER", "amount": 2 } } ] }
        """;

    @Test
    @DisplayName("the declared order is doing work, and a rank orders rules rather than steps")
    void theDeclaredOrderIsDoingWork() {
        ConcurrentList<BuffEvaluator> program = program(ADD_FIFTY, DOUBLE_ON_POST);

        // read off the declared sequence rather than restated here, so that reordering it reaches
        // this test rather than leaving it asserting against a copy of what it used to say
        ConcurrentList<PostProcess.Step> declared = profileRounds();
        ConcurrentList<PostProcess.Step> swapped = reversed(declared);

        // an order that did not actually differ would pass both numbers below while proving nothing
        assertThat(declared.getFirst().round(), is(PostProcess.Round.PET));
        assertThat(swapped.getFirst().round(), is(PostProcess.Round.POST));

        // the bonus round runs ahead of the post one, so the add lands first and the doubling takes
        // it with it
        assertThat(bonus(run(program, declared)), is(closeTo(300.0, EPSILON)));

        // doubling first and adding after is a different number, and that difference is the whole
        // evidence that the sequence is load-bearing rather than decorative
        assertThat(bonus(run(program, swapped)), is(closeTo(250.0, EPSILON)));

        // within one row the rank sorts the add ahead of the multiply, so the file's order of two
        // rules is not a fact that can be wrong
        assertThat(bonus(run(program(addThenScale(true)), only(PostProcess.Round.PET))), is(closeTo(300.0, EPSILON)));
        assertThat(bonus(run(program(addThenScale(false)), only(PostProcess.Round.PET))), is(closeTo(300.0, EPSILON)));

        // and it reaches no further than one row - two rows fold in the order the program holds them,
        // which is why a rank over operations cannot stand in for a sequence over rounds
        assertThat(bonus(run(program(ADD_FIFTY, DOUBLE_ON_BONUS), only(PostProcess.Round.PET))), is(closeTo(300.0, EPSILON)));
        assertThat(bonus(run(program(DOUBLE_ON_BONUS, ADD_FIFTY), only(PostProcess.Round.PET))), is(closeTo(250.0, EPSILON)));
    }

    @Test
    @DisplayName("the pet rounds run profile, then armour, then accessories")
    void thePetRoundsRunProfileThenArmourThenAccessories() {
        assertThat(scopesOf(PostProcess.Round.PET), contains(PostProcess.Scope.PROFILE, PostProcess.Scope.ARMOR, PostProcess.Scope.ACCESSORIES));

        // the POST round is declared as running over the same scopes in the same order, so a change
        // to one of the two that is not made to the other shows up here
        assertThat(scopesOf(PostProcess.Round.POST), is(scopesOf(PostProcess.Round.PET)));

        // a pet expression reads totals the rounds before it settled, so the republish sits ahead of
        // every one of them - and ahead of the POST round for the same reason
        int republish = indexOf(PostProcess.Round.REPUBLISH);
        assertThat(indices(PostProcess.Round.PET), everyItem(is(greaterThan(republish))));
        assertThat(indices(PostProcess.Round.POST), everyItem(is(greaterThan(republish))));

        // and the republish reads a profile table the item rounds and the multiplier have finished
        // writing, which is the one place the order was ever load-bearing before a row existed
        assertThat(indices(PostProcess.Round.ITEM), everyItem(is(lessThan(republish))));
        assertThat(indexOf(PostProcess.Round.MULTIPLIER), is(lessThan(republish)));
    }

    @Test
    @DisplayName("a POST-staged rule is inert in the bonus round and fires in the post one")
    void aPostStagedRuleIsInertInTheBonusRound() {
        ConcurrentList<BuffEvaluator> program = program(DOUBLE_ON_POST);

        // the round is what gates the rule, not the evaluator - the same row, the same program, and
        // the only difference is which stage the step asked for
        assertThat(bonus(run(program, only(PostProcess.Round.PET))), is(closeTo(100.0, EPSILON)));
        assertThat(bonus(run(program, only(PostProcess.Round.POST))), is(closeTo(200.0, EPSILON)));

        // and the mirror, so that neither assertion above passes because the row simply never fires
        ConcurrentList<BuffEvaluator> bonusProgram = program(DOUBLE_ON_BONUS);

        assertThat(bonus(run(bonusProgram, only(PostProcess.Round.PET))), is(closeTo(200.0, EPSILON)));
        assertThat(bonus(run(bonusProgram, only(PostProcess.Round.POST))), is(closeTo(100.0, EPSILON)));
    }

    private static @NotNull String addThenScale(boolean addFirst) {
        String add = "{ \"target\": { \"kind\": \"ALL\" }, \"value\": { \"kind\": \"NUMBER\", \"amount\": 50 } }";
        String scale = "{ \"operation\": \"MULTIPLY\", \"target\": { \"kind\": \"ALL\" }, \"value\": { \"kind\": \"NUMBER\", \"amount\": 2 } }";

        return String.format("{ \"rules\": [ %s, %s ] }", addFirst ? add : scale, addFirst ? scale : add);
    }

    private static @NotNull ConcurrentList<PostProcess.Step> only(@NotNull PostProcess.Round round) {
        return Concurrent.newUnmodifiableList(new PostProcess.Step(round, PostProcess.Scope.PROFILE));
    }

    /**
     * The two rounds the declared sequence runs over the profile table, in the order it declares.
     *
     * <p>
     * The republish is left out because it seeds from the stat repository, so a step list carrying it
     * needs a session - and what is under test here is the order of the two rounds around it rather
     * than the republish itself.
     */
    private static @NotNull ConcurrentList<PostProcess.Step> profileRounds() {
        return PostProcess.STEPS.stream()
            .filter(step -> step.scope() == PostProcess.Scope.PROFILE)
            .filter(step -> step.round() == PostProcess.Round.PET || step.round() == PostProcess.Round.POST)
            .collect(Concurrent.toUnmodifiableList());
    }

    private static @NotNull ConcurrentList<PostProcess.Step> reversed(@NotNull ConcurrentList<PostProcess.Step> steps) {
        ConcurrentList<PostProcess.Step> flipped = Concurrent.newList();

        for (int index = steps.size() - 1; index >= 0; index--)
            flipped.add(steps.get(index));

        return flipped;
    }

    private static @NotNull ConcurrentList<PostProcess.Scope> scopesOf(@NotNull PostProcess.Round round) {
        return PostProcess.STEPS.stream()
            .filter(step -> step.round() == round)
            .map(PostProcess.Step::scope)
            .collect(Concurrent.toUnmodifiableList());
    }

    private static @NotNull ConcurrentList<Integer> indices(@NotNull PostProcess.Round round) {
        ConcurrentList<Integer> found = Concurrent.newList();

        for (int index = 0; index < PostProcess.STEPS.size(); index++) {
            if (PostProcess.STEPS.get(index).round() == round)
                found.add(index);
        }

        return found;
    }

    private static int indexOf(@NotNull PostProcess.Round round) {
        return indices(round).getFirst();
    }

    private static @NotNull ConcurrentList<BuffEvaluator> program(@NotNull String... rows) {
        ConcurrentList<BuffEvaluator> program = Concurrent.newList();

        for (String row : rows)
            program.add(BuffEvaluator.compile(GSON.fromJson(row, Buff.class)));

        return program;
    }

    private static @NotNull StatSheet run(@NotNull ConcurrentList<BuffEvaluator> program, @NotNull ConcurrentList<PostProcess.Step> steps) {
        StatSheet sheet = new StatSheet();
        sheet.add(StatSource.SKILLS, STRENGTH, StatHalf.BONUS, 100.0);
        PostProcess.run(sheet, Concurrent.newMap(), program, ComputeFacts.NONE, steps);

        return sheet;
    }

    private static double bonus(@NotNull StatSheet sheet) {
        return sheet.getProfile().getCell(StatSource.SKILLS, STRENGTH).getBonus();
    }

}
