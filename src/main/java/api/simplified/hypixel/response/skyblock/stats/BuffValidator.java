package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Buff;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentSet;
import dev.simplified.expression.Expression;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The refusals a buff row can only be checked for from this side of the dependency.
 *
 * <p>
 * {@link Buff.Validator} holds everything checkable from the row alone and everything checkable
 * against another corpus table. What is left is the four that name a type declared in this module,
 * and {@code skyblock} can see none of them because the dependency runs the other way. That is one
 * reason rather than several, and it is a fact about which repository declares which name rather
 * than a placement anybody chose.
 *
 * <pre><code>
 *   refusal                                  reads             because otherwise
 *   -------                                  -----             -----------------
 *   a VARIABLE naming no provider constant   VariableProvider  it reads zero on the first profile
 *   a MATH naming an undeclared input        VariableProvider  it throws at build rather than at load
 *   a STAT term naming no origin             StatOrigin        it reads zero, one level further in
 *   a target matching no stat at all         the stat table    a rule can never write, silently
 * </code></pre>
 *
 * <p>
 * The last one looks like a shape refusal and is not, which is worth stating because the difference
 * is not the expression library. Parsing the text would be easy enough to do beside the model; what
 * cannot go there is deciding which of the names it comes back with are legal <b>undeclared</b>. A
 * free name is legal when a provider constant publishes it and a defect when nothing does, so the
 * check is a reading of {@link VariableProvider} that happens to start by parsing.
 *
 * <p>
 * This runs after the corpus has loaded, over rows already bound, never while a row binds - it walks
 * the whole stat table, and resolving a repository from inside a decoder is what the bound layer
 * forbids.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class BuffValidator {

    /**
     * Checks every name a loaded row spells in this module's vocabulary.
     *
     * @param rows every row of the table, already bound
     * @return one message per refusal, empty when every name resolves
     */
    static @NotNull ConcurrentList<String> corpus(@NotNull ConcurrentList<Buff> rows) {
        ConcurrentList<String> refusals = Concurrent.newList();
        ConcurrentSet<String> providers = Concurrent.newSet();
        ConcurrentSet<String> origins = Concurrent.newSet();

        // a provider fanning out over data names a family rather than one variable, so a prefix match
        // is what a corpus-authored name has to be checked against
        for (VariableProvider provider : VariableProvider.values())
            providers.add(provider.name());

        for (StatSource source : StatSource.values())
            origins.add(source.name());

        for (ItemOrigin bucket : ItemOrigin.values())
            origins.add(bucket.name());

        ConcurrentList<Stat> statModels = SkyBlockData.getRepository(Stat.class).findAll();

        for (Buff row : rows) {
            row.getConditions().forEach(condition -> forEachTerm(condition, term -> checkTerm(row, term, providers, origins, refusals)));

            for (Buff.Rule rule : row.getRules()) {
                rule.getWhen().forEach(condition -> forEachTerm(condition, term -> checkTerm(row, term, providers, origins, refusals)));

                if (rule.getValue() != null)
                    forEachTerm(rule.getValue(), term -> checkTerm(row, term, providers, origins, refusals));

                checkTarget(row, rule.getTarget(), statModels, refusals);
            }
        }

        return refusals;
    }

    private static void checkTerm(@NotNull Buff row, @NotNull Buff.Term term, @NotNull ConcurrentSet<String> providers, @NotNull ConcurrentSet<String> origins, @NotNull ConcurrentList<String> refusals) {
        switch (term.getKind()) {
            case VARIABLE -> {
                if (term.getName() != null && providers.stream().noneMatch(provider -> names(provider, term.getName())))
                    refusals.add(refusal(row, String.format("names variable '%s', which no provider constant publishes", term.getName())));
            }
            case STAT -> {
                if (term.getOrigin() != null && !origins.contains(term.getOrigin()))
                    refusals.add(refusal(row, String.format("reads origin '%s', which no source or bucket carries", term.getOrigin())));
            }
            case MATH -> checkMath(row, term, refusals);
            default -> { }
        }
    }

    /**
     * Whether a provider constant publishes a name.
     *
     * <p>
     * Five constants publish their own name and the rest fan out over data - one variable per skill,
     * per dungeon, per collection - under {@code <CONSTANT>_<key>}. Enumerating what they fan out to
     * would copy the corpus into a place it can rot, so a family name is checked by its prefix.
     */
    private static boolean names(@NotNull String provider, @NotNull String name) {
        return provider.equals(name) || name.startsWith(provider + "_");
    }

    private static void checkMath(@NotNull Buff row, @NotNull Buff.Term term, @NotNull ConcurrentList<String> refusals) {
        if (term.getText() == null)
            return;

        try {
            Expression expression = Expression.builder(term.getText())
                .variables(term.getInputs().keySet())
                .build();

            expression.getVariableNames()
                .stream()
                .filter(name -> !term.getInputs().containsKey(name))
                .filter(name -> Arrays.stream(VariableProvider.values()).noneMatch(provider -> names(provider.name(), name)))
                .forEach(name -> refusals.add(refusal(row, String.format("names '%s' in an expression and declares no input for it", name))));
        } catch (RuntimeException exception) {
            refusals.add(refusal(row, String.format("carries an expression that does not parse - %s", exception.getMessage())));
        }
    }

    private static void checkTarget(@NotNull Buff row, Buff.Target target, @NotNull ConcurrentList<Stat> statModels, @NotNull ConcurrentList<String> refusals) {
        // a target every stat falls outside of is a rule that can never write anything, and nothing
        // else would ever say so - the row loads, evaluates and contributes nothing forever
        if (target != null && statModels.stream().noneMatch(target::matches))
            refusals.add(refusal(row, "carries a target that expands to no stat at all"));
    }

    private static void forEachTerm(@NotNull Buff.Condition condition, @NotNull Consumer<Buff.Term> visitor) {
        if (condition.getInput() != null)
            forEachTerm(condition.getInput(), visitor);

        condition.getAll().forEach(nested -> forEachTerm(nested, visitor));
        condition.getAny().forEach(nested -> forEachTerm(nested, visitor));

        if (condition.getNot() != null)
            forEachTerm(condition.getNot(), visitor);
    }

    private static void forEachTerm(@NotNull Buff.Term term, @NotNull Consumer<Buff.Term> visitor) {
        visitor.accept(term);

        for (Map.Entry<String, Buff.Term> entry : term.getInputs())
            forEachTerm(entry.getValue(), visitor);

        Buff.Lookup lookup = term.getLookup();

        if (lookup == null)
            return;

        if (lookup.getOn() != null)
            forEachTerm(lookup.getOn(), visitor);

        if (lookup.getOn2() != null)
            forEachTerm(lookup.getOn2(), visitor);
    }

    private static @NotNull String refusal(@NotNull Buff row, @NotNull String problem) {
        return String.format("buff '%s': %s", row.getId(), problem);
    }

}
