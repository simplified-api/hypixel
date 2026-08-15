package api.simplified.hypixel.response.skyblock.stats.buff;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Buff;
import api.simplified.skyblock.model.Stat;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.RequiredArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.expression.Expression;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.ListTag;
import lib.minecraft.nbt.tag.NumericalTag;
import lib.minecraft.nbt.tag.StringTag;
import lib.minecraft.nbt.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * One buff row folded onto a running number.
 *
 * <p>
 * The row says what it contributes and this says what that comes to. Nothing in the schema evaluates
 * anything - a term carries no {@code evaluate} and a condition carries no {@code test} - so the two
 * switches that read them live here, against a {@link Context} the caller filled.
 *
 * <p>
 * Three properties are worth knowing before reading the fold.
 *
 * <p>
 * <b>An unresolved reading makes a rule inert rather than zero.</b> A term the context has no reading
 * for does not fold, and neither does the rule holding it; the running number is handed back
 * untouched. Folding it as a zero would be a contribution nothing made, and throwing would take out
 * one member's whole compute over one row's arithmetic.
 *
 * <p>
 * <b>Rules fold in operation order, not file order.</b> The adds and subtracts settle, then the
 * scales see the finished sum, then an assignment discards whatever came before it. So two rows in
 * either order in the corpus give the same number and the array order is not semantic - the same
 * property a source already buys by writing through a sink it cannot read back.
 *
 * <p>
 * <b>{@code effects} is shorthand and folds as one.</b> An entry is an add on the value channel, so
 * it settles with the rank-one rules and a scale sees it, which is what makes the shorthand say
 * exactly what a rule would.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class BuffEvaluator {

    private static final @NotNull Comparator<Buff.Rule> BY_RANK = Comparator.comparingInt(rule -> rule.getOperation().getRank());

    /**
     * The row this folds.
     */
    @Getter
    private final @NotNull Buff row;
    private final @NotNull ConcurrentMap<String, Expression> compiled = Concurrent.newMap();

    /**
     * Prepares one row to be folded.
     *
     * @param row the row to read
     * @return the evaluator for it
     */
    public static @NotNull BuffEvaluator compile(@NotNull Buff row) {
        return new BuffEvaluator(row);
    }

    /**
     * Every row attached to one carrier of one kind, plus the rows of that kind attached to every
     * carrier alike.
     *
     * <p>
     * A row is selected on its subject columns and never evaluated to find out whether it is
     * relevant, which is what makes the subject a column triple rather than a condition like any
     * other. Selection walks the table because the table is held; the per-compute index the design
     * describes arrives with the step list that would hold it.
     *
     * @param kind which index to read
     * @param carrierId the carrier's id, empty to select only the carrierless rows
     * @param member the second-level name, empty for a kind that has none
     * @return the rows attached, in the order the table holds them
     */
    public static @NotNull ConcurrentList<Buff> select(@NotNull Buff.Subject.Kind kind, @Nullable String carrierId, @Nullable String member) {
        return SkyBlockData.getRepository(Buff.class)
            .stream()
            .filter(row -> row.getSubjectKind() == kind)
            .filter(row -> row.getSubjectCarrierId() == null || row.getSubjectCarrierId().equals(carrierId))
            .filter(row -> row.getSubjectMember() == null || row.getSubjectMember().equals(member))
            .collect(Concurrent.toUnmodifiableList());
    }

    /**
     * Every row that hangs off no carrier at all.
     *
     * @param kind {@link Buff.Subject.Kind#GROUP} or {@link Buff.Subject.Kind#PROFILE}
     * @return the rows, in the order the table holds them
     */
    public static @NotNull ConcurrentList<Buff> selectAll(@NotNull Buff.Subject.Kind kind) {
        return select(kind, null, null);
    }

    /**
     * Folds this row onto one running number.
     *
     * @param statModel the stat being written
     * @param channel which number of it is being written
     * @param stage which round is running
     * @param current the number as it stands
     * @param context what the row's terms and tests read
     * @return the number to write back, unchanged when the row does not apply
     */
    public double apply(@NotNull Stat statModel, @NotNull Buff.Channel channel, @NotNull Buff.Rule.Stage stage, double current, @NotNull Context context) {
        return this.fold(statModel, channel, stage, null, current, context);
    }

    /**
     * Folds the rules of one scope onto one running number, leaving the rest of the row to whichever
     * table owns them.
     *
     * <p>
     * A scope belongs to a rule rather than to a row, so one row reaches two tables and a caller
     * holding one of them folds only the half addressed to it. A caller holding the only table in
     * play wants every scope at once, and that is what the overload without a scope does.
     *
     * <p>
     * {@link Buff#getEffects()} is shorthand for a rule at the default scope, so it folds in the
     * {@link Buff.Rule.Scope#CARRIER} pass alone - a row carrying an effect beside a profile rule
     * would otherwise contribute that effect to both tables.
     *
     * @param statModel the stat being written
     * @param channel which number of it is being written
     * @param stage which round is running
     * @param scope whose contributions the table being written holds
     * @param current the number as it stands
     * @param context what the row's terms and tests read
     * @return the number to write back, unchanged when no rule of that scope applies
     */
    public double apply(@NotNull Stat statModel, @NotNull Buff.Channel channel, @NotNull Buff.Rule.Stage stage, @NotNull Buff.Rule.Scope scope, double current, @NotNull Context context) {
        return this.fold(statModel, channel, stage, scope, current, context);
    }

    /**
     * Folds this row onto one cell, reading whichever half each rule names and writing the bonus one.
     *
     * <p>
     * A rule reads a half and writes a half, and they are not the same choice: it always writes the
     * bonus, because the base is what a source gave before anything scaled it. So what a rule
     * contributes is the change it made to what it <b>read</b>, and that change lands on the bonus.
     * A {@code TOTAL} rescale therefore preserves the total a whole-cell rescale would produce and
     * differs from it only in the split, which is what the column is for.
     *
     * @param statModel the stat being written
     * @param channel which number of it is being written
     * @param stage which round is running
     * @param scope whose contributions the table being written holds
     * @param base what the source itself gave
     * @param bonus what the bonuses have added
     * @param context what the row's terms and tests read
     * @return the new bonus half, unchanged when no rule applies
     */
    public double applyToCell(@NotNull Stat statModel, @NotNull Buff.Channel channel, @NotNull Buff.Rule.Stage stage, @Nullable Buff.Rule.Scope scope, double base, double bonus, @NotNull Context context) {
        if (!this.holdsAll(this.row.getConditions(), statModel, base + bonus, context))
            return bonus;

        double running = bonus;

        // an effects entry is an add on the value channel at the default scope
        if (channel == Buff.Channel.VALUE && stage == Buff.Rule.Stage.BONUS && (scope == null || scope == Buff.Rule.Scope.CARRIER))
            running += this.row.getEffects().getOrDefault(statModel.getId(), 0.0);

        ConcurrentList<Buff.Rule> applicable = this.applicable(statModel, channel, stage, scope, base + running, context);

        applicable.sort(BY_RANK);

        for (Buff.Rule rule : applicable) {
            double read = switch (rule.getHalf()) {
                case BASE -> base;
                case BONUS -> running;
                case TOTAL -> base + running;
            };

            OptionalDouble operand = this.operand(rule, statModel, read, context);

            // a rule whose operand does not resolve leaves the number alone rather than zeroing it
            if (operand.isEmpty())
                continue;

            running += rule.getOperation().apply(read, operand.getAsDouble()) - read;
        }

        return running;
    }

    /**
     * Whether this row holds any rule addressed to one scope.
     *
     * <p>
     * It reads the row rather than evaluating it, so a caller can skip walking a table no rule of
     * that scope would write - which is the difference between one pass over the profile table and
     * one per carried row per slot.
     *
     * @param scope the scope to look for
     * @return {@code true} when at least one rule carries it
     */
    public boolean writes(@NotNull Buff.Rule.Scope scope) {
        return this.row.getRules().stream().anyMatch(rule -> rule.getScope() == scope);
    }

    /**
     * Folds this row onto an item instance's rarity.
     *
     * <p>
     * A rarity rule carries no target - there is one rarity per instance, so a target would have
     * exactly one legal value - and it resolves before any stat total exists, which is why this takes
     * no stat rather than taking one it would not read.
     *
     * @param ordinal the rarity's position in the ladder, as it stands
     * @param context what the row's terms and tests read
     * @return the position to read the rarity back at
     */
    public double applyRarity(double ordinal, @NotNull Context context) {
        return this.fold(null, Buff.Channel.RARITY, Buff.Rule.Stage.RARITY, null, ordinal, context);
    }

    /**
     * The operations this row would fold onto one channel, without folding any of them.
     *
     * <p>
     * Two of them mean something a number cannot carry back.
     * {@link Buff.Operation#REMOVE} folds nothing and is the identity in an arithmetic step, so a
     * removal is only ever visible where the channel is read - a ceiling that resolved to a number
     * and a ceiling that was taken away are different answers, and an absent cap is already spelled
     * {@code 0.0} on the stat's own column. {@link Buff.Operation#SET} matters for the mirror reason:
     * it is the only operation that means anything against a channel that has no value yet.
     *
     * @param statModel the stat being read
     * @param channel which number of it is being read
     * @param stage which round is running
     * @param context what the row's terms and tests read
     * @return the operations of every rule that applies, empty when the row does not
     */
    public @NotNull ConcurrentList<Buff.Operation> operations(@NotNull Stat statModel, @NotNull Buff.Channel channel, @NotNull Buff.Rule.Stage stage, @NotNull Context context) {
        if (!this.holdsAll(this.row.getConditions(), statModel, 0.0, context))
            return Concurrent.newUnmodifiableList();

        return this.applicable(statModel, channel, stage, null, 0.0, context)
            .stream()
            .map(Buff.Rule::getOperation)
            .collect(Concurrent.toUnmodifiableList());
    }

    private double fold(@Nullable Stat statModel, @NotNull Buff.Channel channel, @NotNull Buff.Rule.Stage stage, @Nullable Buff.Rule.Scope scope, double current, @NotNull Context context) {
        if (!this.holdsAll(this.row.getConditions(), statModel, current, context))
            return current;

        double value = current;

        // an effects entry is an add on the value channel at the default scope, so it settles with
        // the rank-one rules and a pass folding one scope reads it only where that scope is the one
        if (statModel != null && channel == Buff.Channel.VALUE && stage == Buff.Rule.Stage.BONUS && (scope == null || scope == Buff.Rule.Scope.CARRIER))
            value += this.row.getEffects().getOrDefault(statModel.getId(), 0.0);

        ConcurrentList<Buff.Rule> applicable = this.applicable(statModel, channel, stage, scope, current, context);

        applicable.sort(BY_RANK);

        for (Buff.Rule rule : applicable) {
            OptionalDouble operand = this.operand(rule, statModel, current, context);

            // a rule whose operand does not resolve leaves the number alone rather than zeroing it
            if (operand.isEmpty())
                continue;

            value = rule.getOperation().apply(value, operand.getAsDouble());
        }

        return value;
    }

    private @NotNull ConcurrentList<Buff.Rule> applicable(@Nullable Stat statModel, @NotNull Buff.Channel channel, @NotNull Buff.Rule.Stage stage, @Nullable Buff.Rule.Scope scope, double current, @NotNull Context context) {
        return this.row.getRules()
            .stream()
            .filter(rule -> rule.getStage() == stage)
            .filter(rule -> rule.getChannels().contains(channel))
            // an absent scope folds every rule, which is what a caller holding the only table in
            // play wants - and what every caller wanted before a rule could name a second one
            .filter(rule -> scope == null || rule.getScope() == scope)
            .filter(rule -> statModel == null || (rule.getTarget() != null && rule.getTarget().matches(statModel)))
            .filter(rule -> statModel == null || rule.getOperation().appliesTo(statModel))
            .filter(rule -> this.holdsAll(rule.getWhen(), statModel, current, context))
            .collect(Concurrent.toList());
    }

    private @NotNull OptionalDouble operand(@NotNull Buff.Rule rule, @Nullable Stat statModel, double current, @NotNull Context context) {
        if (rule.getOperation() == Buff.Operation.REMOVE)
            return OptionalDouble.of(0.0);

        if (rule.getValue() == null)
            return OptionalDouble.empty();

        OptionalDouble evaluated = this.number(rule.getValue(), statModel, current, context);

        if (evaluated.isEmpty())
            return evaluated;

        double bounded = evaluated.getAsDouble();

        // a ceiling and a floor bound this one rule's contribution, before the operation folds it
        if (rule.getCeiling() != null)
            bounded = Math.min(bounded, rule.getCeiling());

        if (rule.getFloor() != null)
            bounded = Math.max(bounded, rule.getFloor());

        return OptionalDouble.of(rule.getOperation().isUnitBearing() ? rule.getUnit().toFactor(bounded) : bounded);
    }

    // --- Terms ---

    private @NotNull OptionalDouble number(@NotNull Buff.Term term, @Nullable Stat statModel, double current, @NotNull Context context) {
        return switch (term.getKind()) {
            case NUMBER -> term.getAmount() == null ? OptionalDouble.empty() : OptionalDouble.of(term.getAmount());
            case CURRENT -> OptionalDouble.of(current);
            case VARIABLE -> term.getName() == null ? OptionalDouble.empty() : context.variable(term.getName());
            case STAT -> this.statTotal(term, statModel, context);
            case TAG -> this.tagNumber(term, context);
            case CARRIER -> term.getCarrier() == null ? OptionalDouble.empty() : context.carrierNumber(term.getCarrier());
            case COUNT -> term.getGroup() == null ? OptionalDouble.empty() : toDouble(context.groupCount(term.getGroup()));
            case WORLD -> term.getWorld() == null ? OptionalDouble.empty() : context.worldNumber(term.getWorld());
            case PLAYER -> term.getPlayer() == null ? OptionalDouble.empty() : context.playerNumber(term.getPlayer());
            case LOOKUP -> this.lookup(term, statModel, current, context);
            case MATH -> this.math(term, statModel, current, context);
        };
    }

    private @NotNull Optional<String> key(@NotNull Buff.Term term, @Nullable Stat statModel, double current, @NotNull Context context) {
        return switch (term.getKind()) {
            case TAG -> this.tagText(term, context);
            case CARRIER -> term.getCarrier() == null ? Optional.empty() : context.carrierKey(term.getCarrier());
            case WORLD -> term.getWorld() == null ? Optional.empty() : context.worldKey(term.getWorld());
            default -> this.number(term, statModel, current, context).stream().mapToObj(BuffEvaluator::asKey).findFirst();
        };
    }

    private @NotNull OptionalDouble statTotal(@NotNull Buff.Term term, @Nullable Stat statModel, @NotNull Context context) {
        if (term.getStat() == null)
            return OptionalDouble.empty();

        // TARGET is whichever stat the rule is writing, which is what lets one row scale every stat
        // by its own pet contribution rather than by a stat it names
        if (!"TARGET".equals(term.getStat()))
            return context.stat(term.getStat(), term.getOrigin());

        // nothing is being written on the rarity channel, so there is no target to be
        return statModel == null ? OptionalDouble.empty() : context.stat(statModel.getId(), term.getOrigin());
    }

    private @NotNull OptionalDouble tagNumber(@NotNull Buff.Term term, @NotNull Context context) {
        Optional<Tag<?>> found = this.tag(term, context);

        if (found.isEmpty())
            return OptionalDouble.empty();

        Buff.Term.Read read = term.getRead() == null ? Buff.Term.Read.NUMBER : term.getRead();
        Tag<?> tag = found.get();

        if (read == Buff.Term.Read.COUNT)
            return OptionalDouble.of(count(tag, term.getMatching()));

        if (tag instanceof NumericalTag<?> numericalTag)
            return OptionalDouble.of(numericalTag.getValue().doubleValue());

        return OptionalDouble.empty();
    }

    private @NotNull Optional<String> tagText(@NotNull Buff.Term term, @NotNull Context context) {
        return this.tag(term, context)
            .filter(StringTag.class::isInstance)
            .map(tag -> ((StringTag) tag).getValue());
    }

    private @NotNull Optional<Tag<?>> tag(@NotNull Buff.Term term, @NotNull Context context) {
        if (term.getPath() == null)
            return Optional.empty();

        // a declared path can tell an absent tag from a zero, which is the whole reason it is a term
        // rather than text spliced into an expression
        return context.getTag().map(compoundTag -> compoundTag.getPath(term.getPath()));
    }

    private @NotNull OptionalDouble lookup(@NotNull Buff.Term term, @Nullable Stat statModel, double current, @NotNull Context context) {
        Buff.Lookup table = term.getLookup();

        if (table == null || table.getOn() == null)
            return OptionalDouble.empty();

        if (table.getOn2() == null)
            return this.read(table, table.getAmounts(), table.getOn(), statModel, current, context);

        Optional<String> outer = this.key(table.getOn(), statModel, current, context);

        return outer.map(table.getAmountsBy2()::get)
            .map(inner -> this.read(table, inner, table.getOn2(), statModel, current, context))
            .orElse(OptionalDouble.empty());
    }

    private @NotNull OptionalDouble read(@NotNull Buff.Lookup table, @NotNull ConcurrentMap<String, Double> amounts, @NotNull Buff.Term axis, @Nullable Stat statModel, double current, @NotNull Context context) {
        if (table.getMode() == Buff.Lookup.Mode.EXACT) {
            return this.key(axis, statModel, current, context)
                .map(amounts::get)
                .map(OptionalDouble::of)
                .orElse(OptionalDouble.empty());
        }

        OptionalDouble input = this.number(axis, statModel, current, context);

        if (input.isEmpty())
            return input;

        return switch (table.getMode()) {
            case THRESHOLD -> amounts.stream()
                .filter(entry -> reached(entry.getKey(), input.getAsDouble()))
                .max(Comparator.comparingDouble(entry -> rung(entry.getKey())))
                .map(entry -> OptionalDouble.of(entry.getValue()))
                .orElse(OptionalDouble.empty());
            case THRESHOLD_CUMULATIVE -> OptionalDouble.of(amounts.stream()
                .filter(entry -> reached(entry.getKey(), input.getAsDouble()))
                .mapToDouble(Map.Entry::getValue)
                .sum());
            case INTERPOLATE -> interpolate(amounts, input.getAsDouble());
            case EXACT -> OptionalDouble.empty();
        };
    }

    private @NotNull OptionalDouble math(@NotNull Buff.Term term, @Nullable Stat statModel, double current, @NotNull Context context) {
        if (term.getText() == null)
            return OptionalDouble.empty();

        ConcurrentMap<String, Double> inputs = Concurrent.newMap();

        for (Map.Entry<String, Buff.Term> entry : term.getInputs()) {
            OptionalDouble value = this.number(entry.getValue(), statModel, current, context);

            // one input the context cannot read leaves the whole expression unevaluable
            if (value.isEmpty())
                return OptionalDouble.empty();

            inputs.put(entry.getKey(), value.getAsDouble());
        }

        Expression expression = this.compiled.computeIfAbsent(
            term.getText(),
            text -> Expression.builder(text)
                .variables(context.getVariables().keySet())
                .variables(term.getInputs().keySet())
                .build()
        );

        return OptionalDouble.of(expression.setVariables(context.getVariables()).setVariables(inputs).evaluate());
    }

    // --- Conditions ---

    private boolean holdsAll(@NotNull ConcurrentList<Buff.Condition> conditions, @Nullable Stat statModel, double current, @NotNull Context context) {
        return conditions.stream().allMatch(condition -> this.holds(condition, statModel, current, context));
    }

    private boolean holds(@NotNull Buff.Condition condition, @Nullable Stat statModel, double current, @NotNull Context context) {
        if (!this.holdsAll(condition.getAll(), statModel, current, context))
            return false;

        if (condition.getAny().notEmpty() && condition.getAny().stream().noneMatch(nested -> this.holds(nested, statModel, current, context)))
            return false;

        if (condition.getNot() != null && this.holds(condition.getNot(), statModel, current, context))
            return false;

        if (condition.getInput() == null || condition.getOp() == null)
            return true;

        return this.compares(condition, statModel, current, context);
    }

    private boolean compares(@NotNull Buff.Condition condition, @Nullable Stat statModel, double current, @NotNull Context context) {
        Buff.Term input = condition.getInput();

        if (input == null || condition.getOp() == null)
            return false;

        return switch (condition.getOp()) {
            case EQ -> this.matches(condition, input, statModel, current, context);
            case NE -> !this.matches(condition, input, statModel, current, context);
            case IN -> this.key(input, statModel, current, context).filter(condition.getKeys()::contains).isPresent();
            case NOT_IN -> this.key(input, statModel, current, context).map(key -> !condition.getKeys().contains(key)).orElse(false);
            case WITHIN -> this.number(input, statModel, current, context)
                .stream()
                .anyMatch(value -> condition.getRanges().stream().anyMatch(range -> range.contains((int) value)));
            case GT, GTE, LT, LTE -> this.orders(condition, input, statModel, current, context);
        };
    }

    private boolean matches(@NotNull Buff.Condition condition, @NotNull Buff.Term input, @Nullable Stat statModel, double current, @NotNull Context context) {
        if (condition.getKey() != null)
            return this.key(input, statModel, current, context).filter(condition.getKey()::equals).isPresent();

        if (condition.getAmount() == null)
            return false;

        return this.number(input, statModel, current, context).stream().anyMatch(value -> value == condition.getAmount());
    }

    private boolean orders(@NotNull Buff.Condition condition, @NotNull Buff.Term input, @Nullable Stat statModel, double current, @NotNull Context context) {
        if (condition.getAmount() == null)
            return false;

        OptionalDouble value = this.number(input, statModel, current, context);

        if (value.isEmpty())
            return false;

        double left = value.getAsDouble();
        double right = condition.getAmount();

        return switch (condition.getOp()) {
            case GT -> left > right;
            case GTE -> left >= right;
            case LT -> left < right;
            case LTE -> left <= right;
            default -> false;
        };
    }

    // --- Readings ---

    /**
     * Counts what a tag holds, optionally only the entries equal to a value.
     *
     * <p>
     * A compound counts by its values rather than its keys, because what a counter on an instance
     * asks is how many slots hold a thing rather than how many slots there are. An entry whose value
     * is itself a compound compares equal to nothing, so a nested encoding of the same fact is not
     * counted - which is the reading the Java this replaces already had.
     */
    private static double count(@NotNull Tag<?> tag, @Nullable String matching) {
        if (tag instanceof ListTag<?> listTag)
            return matching == null ? listTag.size() : matching(listTag.stream().map(Tag::getValue), matching);

        if (tag instanceof CompoundTag compoundTag)
            return matching == null ? compoundTag.size() : matching(compoundTag.values().stream().map(Tag::getValue), matching);

        return 0.0;
    }

    private static double matching(@NotNull Stream<?> values, @NotNull String matching) {
        return values.filter(matching::equals).count();
    }

    /**
     * Reads a table key as the number a threshold ladder climbs, treating an unreadable key as a rung
     * nothing reaches.
     */
    private static double rung(@NotNull String key) {
        try {
            return Double.parseDouble(key);
        } catch (NumberFormatException ignore) {
            return Double.POSITIVE_INFINITY;
        }
    }

    /**
     * Whether an input has climbed to a rung, at equality rather than past it.
     */
    private static boolean reached(@NotNull String key, double input) {
        return input >= rung(key);
    }

    private static @NotNull OptionalDouble interpolate(@NotNull ConcurrentMap<String, Double> amounts, double input) {
        Map.Entry<String, Double> below = null;
        Map.Entry<String, Double> above = null;

        for (Map.Entry<String, Double> entry : amounts) {
            double rung = rung(entry.getKey());

            if (rung <= input && (below == null || rung > rung(below.getKey())))
                below = entry;

            if (rung >= input && (above == null || rung < rung(above.getKey())))
                above = entry;
        }

        // flat outside the ends rather than extrapolated, which is what a ladder's last rung means
        if (below == null)
            return above == null ? OptionalDouble.empty() : OptionalDouble.of(above.getValue());

        if (above == null)
            return OptionalDouble.of(below.getValue());

        double span = rung(above.getKey()) - rung(below.getKey());

        if (span == 0.0)
            return OptionalDouble.of(below.getValue());

        double fraction = (input - rung(below.getKey())) / span;
        return OptionalDouble.of(below.getValue() + (fraction * (above.getValue() - below.getValue())));
    }

    /**
     * Spells a number the way a table key spells one, so a whole number does not look up as
     * {@code 5.0} against a rung written {@code 5}.
     */
    private static @NotNull String asKey(double value) {
        return value == Math.rint(value) && !Double.isInfinite(value)
            ? String.valueOf((long) value)
            : String.valueOf(value);
    }

    private static @NotNull OptionalDouble toDouble(@NotNull OptionalInt value) {
        return value.isPresent() ? OptionalDouble.of(value.getAsInt()) : OptionalDouble.empty();
    }

    /**
     * Everything a buff row can read, resolved by whoever knows how and looked up by the evaluator.
     *
     * <p>
     * It is a bag of readings rather than an interface over the player, and the difference is the
     * whole of how an unsupported term behaves. A reading nobody filled in is <b>absent</b>, not zero
     * - so a rule naming it does not fold, and the row is left inert and countable rather than folded
     * in as a contribution of nothing. Every {@link Buff.Term.World} and {@link Buff.Term.Player}
     * reading is in that state today, because nothing on the wire carries one.
     *
     * <p>
     * A caller fills only what its subject has:
     *
     * <pre><code>
     *   subject     fills
     *   -------     -----
     *   ITEM        the carrier readings off the instance, and its tag
     *   PET_PERK    the carrier readings off the resolved pet
     *   GROUP       the group counts
     *   PROFILE     the variables, and nothing else
     * </code></pre>
     *
     * <p>
     * Stat totals are read through the variables under {@code STAT_<statId>}, and under
     * {@code STAT_<origin>_<statId>} for a term naming one origin, which is the convention the pass
     * boundary publishes them by.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Context {

        /**
         * Every variable, which is what an expression is built against.
         */
        @Getter
        private final @NotNull ConcurrentMap<String, Double> variables;
        private final @NotNull ConcurrentMap<Buff.Term.Carrier, String> carrierKeys = Concurrent.newMap();
        private final @NotNull ConcurrentMap<Buff.Term.Carrier, Double> carrierNumbers = Concurrent.newMap();
        private final @NotNull ConcurrentMap<Buff.Term.World, String> worldKeys = Concurrent.newMap();
        private final @NotNull ConcurrentMap<Buff.Term.World, Double> worldNumbers = Concurrent.newMap();
        private final @NotNull ConcurrentMap<Buff.Term.Player, Double> playerNumbers = Concurrent.newMap();
        private final @NotNull ConcurrentMap<String, Integer> groupCounts = Concurrent.newMap();
        private @Nullable CompoundTag tag;

        /**
         * Opens a context over the expression variables a compute has published.
         *
         * @param variables the seeded variables and the published stat totals
         * @return a context filled with nothing else
         */
        public static @NotNull Context of(@NotNull ConcurrentMap<String, Double> variables) {
            return new Context(variables);
        }

        /**
         * Fills one key-valued property of the carrier.
         *
         * @param property which property
         * @param value what it reads
         * @return this context
         */
        public @NotNull Context carrier(@NotNull Buff.Term.Carrier property, @Nullable String value) {
            if (value != null && !value.isEmpty())
                this.carrierKeys.put(property, value);

            return this;
        }

        /**
         * Fills one numeric property of the carrier.
         *
         * @param property which property
         * @param value what it reads
         * @return this context
         */
        public @NotNull Context carrier(@NotNull Buff.Term.Carrier property, double value) {
            this.carrierNumbers.put(property, value);
            return this;
        }

        /**
         * Fills one key-valued property of the world.
         *
         * @param property which property
         * @param value what it reads
         * @return this context
         */
        public @NotNull Context world(@NotNull Buff.Term.World property, @Nullable String value) {
            if (value != null && !value.isEmpty())
                this.worldKeys.put(property, value);

            return this;
        }

        /**
         * Fills one numeric property of the world.
         *
         * @param property which property
         * @param value what it reads
         * @return this context
         */
        public @NotNull Context world(@NotNull Buff.Term.World property, double value) {
            this.worldNumbers.put(property, value);
            return this;
        }

        /**
         * Fills one numeric property of the live player.
         *
         * @param property which property
         * @param value what it reads
         * @return this context
         */
        public @NotNull Context player(@NotNull Buff.Term.Player property, double value) {
            this.playerNumbers.put(property, value);
            return this;
        }

        /**
         * Fills the carrier's NBT, which is what a tag term reads a path out of.
         *
         * @param compoundTag the carrier's tag
         * @return this context
         */
        public @NotNull Context tag(@Nullable CompoundTag compoundTag) {
            this.tag = compoundTag;
            return this;
        }

        /**
         * Fills how many members of one declared group the member is wearing.
         *
         * @param groupId the group's id
         * @param count how many are worn
         * @return this context
         */
        public @NotNull Context groupCount(@NotNull String groupId, int count) {
            this.groupCounts.put(groupId, count);
            return this;
        }

        /**
         * One seeded expression variable.
         *
         * @param name the variable's name
         * @return its value, empty when nothing published it
         */
        public @NotNull OptionalDouble variable(@NotNull String name) {
            Double value = this.variables.get(name);
            return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
        }

        /**
         * One stat's published total.
         *
         * @param statId the stat to read
         * @param origin the origin to read it under, null for every origin together
         * @return the total, empty when nothing published it
         */
        public @NotNull OptionalDouble stat(@NotNull String statId, @Nullable String origin) {
            return this.variable(origin == null
                ? String.format("STAT_%s", statId)
                : String.format("STAT_%s_%s", origin, statId));
        }

        /**
         * One key-valued property of the carrier.
         *
         * @param property which property
         * @return what it reads, empty when the carrier has no such property
         */
        public @NotNull Optional<String> carrierKey(@NotNull Buff.Term.Carrier property) {
            return Optional.ofNullable(this.carrierKeys.get(property));
        }

        /**
         * One numeric property of the carrier.
         *
         * @param property which property
         * @return what it reads, empty when the carrier has no such property
         */
        public @NotNull OptionalDouble carrierNumber(@NotNull Buff.Term.Carrier property) {
            Double value = this.carrierNumbers.get(property);
            return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
        }

        /**
         * One key-valued property of the world.
         *
         * @param property which property
         * @return what it reads, empty because nothing supplies one yet
         */
        public @NotNull Optional<String> worldKey(@NotNull Buff.Term.World property) {
            return Optional.ofNullable(this.worldKeys.get(property));
        }

        /**
         * One numeric property of the world.
         *
         * @param property which property
         * @return what it reads, empty because nothing supplies one yet
         */
        public @NotNull OptionalDouble worldNumber(@NotNull Buff.Term.World property) {
            Double value = this.worldNumbers.get(property);
            return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
        }

        /**
         * One numeric property of the live player.
         *
         * @param property which property
         * @return what it reads, empty because nothing supplies one yet
         */
        public @NotNull OptionalDouble playerNumber(@NotNull Buff.Term.Player property) {
            Double value = this.playerNumbers.get(property);
            return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
        }

        /**
         * How many members of one declared group the member is wearing.
         *
         * <p>
         * A group the caller filled reads its count and a group it did not is absent rather than
         * zero. The two are different: a declared group nobody is wearing is a real count of none,
         * and an unfilled one is a reading this context cannot make. A group no row declares never
         * reaches here at all - that is refused when the corpus is read.
         *
         * @param groupId the group's id
         * @return how many are worn, empty when this context has no count for the group
         */
        public @NotNull OptionalInt groupCount(@NotNull String groupId) {
            Integer count = this.groupCounts.get(groupId);
            return count == null ? OptionalInt.empty() : OptionalInt.of(count);
        }

        /**
         * The carrier's NBT.
         */
        public @NotNull Optional<CompoundTag> getTag() {
            return Optional.ofNullable(this.tag);
        }

    }

}
