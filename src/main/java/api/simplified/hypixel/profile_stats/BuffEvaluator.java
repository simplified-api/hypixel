package api.simplified.hypixel.profile_stats;

import api.simplified.skyblock.model.BuffEffectsModel;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.expression.Expression;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.IntTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One reference row's whole stat payload, parsed once and applied many times.
 * <p>
 * A row's key is split and its value classified when the row is compiled, not on every call, so one
 * expression text is parsed once however many stats it applies to. The expression is the value alone
 * and evaluates to an operand; what to do with that operand is the prefix's business and is done in
 * Java.
 */
public final class BuffEvaluator {

    private static final Pattern NBT_REFERENCE = Pattern.compile("nbt_([a-zA-Z0-9_\\-.]+)");

    /**
     * The name an expression uses for the running total.
     */
    public static final String CURRENT_VALUE = "CURRENT_VALUE";

    /**
     * The name an expression uses for the active pet's total in whichever stat is being totalled.
     */
    public static final String PET_ALL = "PET_ALL";

    private final @NotNull Map<String, Double> effects;
    private final @NotNull ConcurrentList<BuffRule> rules;
    private final @NotNull ConcurrentList<HourRange> timeGuard;
    private final @NotNull ConcurrentMap<String, Expression> compiled = Concurrent.newMap();

    private BuffEvaluator(@NotNull Map<String, Double> effects, @NotNull ConcurrentList<BuffRule> rules, @NotNull ConcurrentList<HourRange> timeGuard) {
        this.effects = effects;
        this.rules = rules;
        this.timeGuard = timeGuard;
    }

    /**
     * Parses a reference row's {@code effects} and {@code buff_effects} payloads.
     *
     * @param buffEffectsModel the row to read
     * @return the compiled evaluator
     */
    @SuppressWarnings("unchecked")
    public static @NotNull BuffEvaluator compile(@NotNull BuffEffectsModel buffEffectsModel) {
        ConcurrentList<BuffRule> rules = Concurrent.newList();
        ConcurrentList<HourRange> timeGuard = Concurrent.newList();

        buffEffectsModel.getBuffEffects().forEach((buffKey, buffValue) -> {
            if ("TIME".equals(buffKey)) {
                ((List<String>) buffValue).forEach(range -> timeGuard.add(HourRange.parse(range)));
                return;
            }

            rules.add(BuffRule.parse(buffKey, buffValue));
        });

        return new BuffEvaluator(buffEffectsModel.getEffects(), rules, timeGuard);
    }

    /**
     * Substitutes every {@code nbt_<path>} reference against one item's tag.
     * <p>
     * The paths are item-scoped rather than stat-scoped, so a row is bound once per item and every
     * reference it names goes at once - a value naming two paths substitutes both.
     *
     * @param compoundTag the item's tag, null for a source that has none
     * @return an evaluator whose expressions name no nbt reference
     */
    public @NotNull BuffEvaluator bind(@Nullable CompoundTag compoundTag) {
        if (compoundTag == null)
            return this;

        ConcurrentList<BuffRule> bound = this.rules
            .stream()
            .map(rule -> rule.getExpressionText() == null ? rule : rule.withExpressionText(substitute(rule.getExpressionText(), compoundTag)))
            .collect(Concurrent.toList());

        return new BuffEvaluator(this.effects, bound, this.timeGuard);
    }

    /**
     * Applies this row to one running total.
     *
     * @param statModel the stat being totalled
     * @param current the value accumulated so far
     * @param variables the player state the expressions are evaluated against
     * @param pass which pass is running
     * @return the adjusted total
     */
    public double apply(@NotNull Stat statModel, double current, @NotNull ConcurrentMap<String, Double> variables, @NotNull Operation.Pass pass) {
        // a guard that holds within no range at all zeroes the value rather than leaving it alone
        if (this.timeGuard.notEmpty() && this.timeGuard.stream().noneMatch(HourRange::containsNow))
            return 0.0;

        double value = current + this.effects.getOrDefault(statModel.getId(), 0.0);

        for (BuffRule rule : this.rules) {
            if (rule.getOperation().getPass() != pass || !rule.matches(statModel))
                continue;

            value = rule.getOperation().apply(value, this.operand(rule, statModel, current, variables));
        }

        return value;
    }

    private double operand(@NotNull BuffRule rule, @NotNull Stat statModel, double current, @NotNull ConcurrentMap<String, Double> variables) {
        if (rule.getConstant() != null)
            return rule.getConstant();

        return this.compiled.computeIfAbsent(
                rule.getExpressionText(),
                text -> Expression.builder(text)
                    .variables(variables.keySet())
                    .variable(CURRENT_VALUE)
                    .variable(PET_ALL)
                    .build()
            )
            .setVariables(variables)
            .setVariable(CURRENT_VALUE, current)
            .setVariable(PET_ALL, variables.getOrDefault(String.format("STAT_PET_%s", statModel.getId()), 0.0))
            .evaluate();
    }

    private static @NotNull String substitute(@NotNull String expressionText, @NotNull CompoundTag compoundTag) {
        Matcher matcher = NBT_REFERENCE.matcher(expressionText);
        String substituted = expressionText;

        while (matcher.find()) {
            String reference = matcher.group();
            String value = String.valueOf(compoundTag.getPathOrDefault(matcher.group(1), IntTag.EMPTY).getValue());
            substituted = substituted.replace(reference, value);
        }

        return substituted;
    }

}
