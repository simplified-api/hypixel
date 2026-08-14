package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.stats.buff.BuffEvaluator;
import api.simplified.skyblock.model.Buff;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * One stat's ceiling, as it stands for one member.
 *
 * <p>
 * A cap is computed rather than read. {@link Stat#getCap()} is where it starts and every rule writing
 * the {@link Buff.Channel#CAP} channel moves it from there, so two members wearing different gear have
 * different ceilings for the same stat. It is optional because uncapped is a real answer rather than a
 * large number - the stat column already spells uncapped as {@code 0.0}, and a rule can take a ceiling
 * away that a member otherwise had.
 *
 * <p>
 * Nothing here rewrites a cell. A cap is applied where a total is read, so the sheet holds what each
 * source contributed and the ceiling is a property of reading them together.
 *
 * @param ceiling the ceiling, empty when the stat is uncapped for this member
 */
public record StatCap(@NotNull Optional<Double> ceiling) {

    /**
     * The answer for a stat with no ceiling, which is most of them - 81 of the corpus's 88 stat rows
     * leave the column at its default.
     */
    public static final @NotNull StatCap UNCAPPED = new StatCap(Optional.empty());

    /**
     * Resolves one stat's ceiling from its own column and every cap-writing rule that applies.
     *
     * <p>
     * Three readings are taken here that nothing else states, because the code takes one whether or
     * not anybody has ruled. Raises are <b>additive</b>, so two of them stack. A {@code SET} is a
     * <b>replacement</b> rather than a downward clamp, which falls out of the operation rank rather
     * than being chosen separately - assignments are rank three and fold after every raise. And a stat
     * whose own column is uncapped <b>stays uncapped under a raise</b>, because a raise is relative to
     * a ceiling and there is none to be relative to; only a {@code SET} gives such a stat one.
     *
     * @param statModel the stat to resolve
     * @param program every cap-writing evaluator, each with the context of the carrier it came from
     * @return the resolved ceiling
     */
    static @NotNull StatCap resolve(@NotNull Stat statModel, @NotNull ConcurrentList<Scoped> program) {
        ConcurrentList<Buff.Operation> operations = Concurrent.newList();

        program.forEach(scoped -> operations.addAll(scoped.evaluator().operations(statModel, Buff.Channel.CAP, Buff.Rule.Stage.BONUS, scoped.context())));

        // a removal is not a number, so it is answered before any arithmetic runs rather than by one
        if (operations.contains(Buff.Operation.REMOVE))
            return UNCAPPED;

        double base = statModel.getCap();

        // a raise is relative to a ceiling and an uncapped stat has none to be relative to, so only
        // an assignment gives one a ceiling. Crit Chance reads 152 percent against no column at all,
        // and a raise landing on it would invent a cap the game does not have
        if (base <= 0.0 && !operations.contains(Buff.Operation.SET))
            return UNCAPPED;

        double resolved = base;

        for (Scoped scoped : program)
            resolved = scoped.evaluator().apply(statModel, Buff.Channel.CAP, Buff.Rule.Stage.BONUS, resolved, scoped.context());

        return resolved > 0.0 ? new StatCap(Optional.of(resolved)) : UNCAPPED;
    }

    /**
     * Clamps one total to this ceiling.
     *
     * @param total the number as the sources add up to it
     * @return the total, or the ceiling when the total is over it
     */
    public double clamp(double total) {
        return this.ceiling.map(ceiling -> Math.min(total, ceiling)).orElse(total);
    }

    /**
     * Whether this stat has no ceiling for the member it was resolved against.
     *
     * @return {@code true} when nothing clamps
     */
    public boolean isUncapped() {
        return this.ceiling.isEmpty();
    }

    /**
     * One evaluator with the context of whatever carries it.
     *
     * <p>
     * A ceiling belongs to the member while the rule raising it belongs to a piece of gear, so the two
     * travel together - the rule reads its own carrier's rarity and level, and what it writes is the
     * member's.
     *
     * @param evaluator the row, compiled
     * @param context what that row's terms and tests read
     */
    record Scoped(@NotNull BuffEvaluator evaluator, @NotNull BuffEvaluator.Context context) {
    }

}
