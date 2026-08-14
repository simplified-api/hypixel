package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.stats.buff.BuffEvaluator;
import api.simplified.skyblock.model.Buff;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * The bonus pass: every contribution whose value is expressed against the flat totals.
 * <p>
 * The sequence is a value rather than a run of statements, so what the pass does in what order is
 * something a caller reads and a test varies. {@link #STEPS} is that sequence and running the pass is
 * running it in order.
 * <p>
 * The order is load-bearing in three places and only three: the republish sits between the
 * enchantment multiplier and the pet rounds, so a pet expression reads post-multiplier totals; the pet
 * rounds rescale item tables the item rounds have already written; and the {@code POST} round runs
 * over tables the {@code BONUS} round has settled. Two steps over disjoint tables commute, and most
 * pairs here are disjoint.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PostProcess {

    /**
     * The pass, in order.
     * <p>
     * A step's scope names what it ranges over rather than what it writes, which is the whole of the
     * difference between {@link Round#MULTIPLIER} and the rest - it ranges over armour and rescales
     * the profile table.
     */
    static final @NotNull ConcurrentList<Step> STEPS = Concurrent.newUnmodifiableList(
        new Step(Round.ITEM, Scope.ACCESSORIES),
        new Step(Round.ITEM, Scope.ARMOR),
        new Step(Round.MULTIPLIER, Scope.ARMOR),
        new Step(Round.REPUBLISH, Scope.PROFILE),
        new Step(Round.PET, Scope.PROFILE),
        new Step(Round.PET, Scope.ARMOR),
        new Step(Round.PET, Scope.ACCESSORIES),
        new Step(Round.POST, Scope.PROFILE),
        new Step(Round.POST, Scope.ARMOR),
        new Step(Round.POST, Scope.ACCESSORIES)
    );

    /**
     * Runs the declared pass.
     *
     * @param sheet the profile's own table and one per filled item slot
     * @param variables the expression variables, republished as each round settles
     * @param program the rows the summoned pet's perks carry, one evaluator each
     */
    public static void run(
        @NotNull StatSheet sheet,
        @NotNull ConcurrentMap<String, Double> variables,
        @NotNull ConcurrentList<BuffEvaluator> program
    ) {
        run(sheet, variables, program, STEPS);
    }

    /**
     * Runs a given sequence, which is what lets a test assert the declared one is doing work.
     *
     * @param sheet the profile's own table and one per filled item slot
     * @param variables the expression variables, republished as each round settles
     * @param program the rows the summoned pet's perks carry, one evaluator each
     * @param steps the sequence to run, in order
     */
    static void run(
        @NotNull StatSheet sheet,
        @NotNull ConcurrentMap<String, Double> variables,
        @NotNull ConcurrentList<BuffEvaluator> program,
        @NotNull ConcurrentList<Step> steps
    ) {
        steps.forEach(step -> step.round().run(sheet, variables, program, step.scope()));
    }

    /**
     * Every buff row the summoned pet's perks carry, one evaluator each, empty when none is summoned.
     *
     * @param context the member and the island to read from
     * @return the program the pet rounds fold
     */
    static @NotNull ConcurrentList<BuffEvaluator> program(@NotNull StatContext context) {
        return ResolvedPet.of(context.getMember())
            .map(PostProcess::petPerkRows)
            .orElseGet(Concurrent::newUnmodifiableList);
    }

    /**
     * One step of the pass.
     *
     * @param round what the step does
     * @param scope what it ranges over
     */
    record Step(@NotNull Round round, @NotNull Scope scope) {
    }

    /**
     * What the tables a step ranges over are.
     */
    enum Scope {

        /**
         * The member's own table, which no item slot backs.
         */
        PROFILE,

        /**
         * The four worn armour pieces.
         */
        ARMOR,

        /**
         * Every counting accessory out of the bag.
         */
        ACCESSORIES;

        /**
         * The slots this scope names, empty for {@link #PROFILE}, which is not a slot.
         *
         * @param sheet the sheet to read the slots out of
         * @return the slots, in the order the sheet opened them
         */
        @NotNull ConcurrentList<StatSheet.Slot> slots(@NotNull StatSheet sheet) {
            return switch (this) {
                case PROFILE -> Concurrent.newUnmodifiableList();
                case ARMOR -> sheet.getSlots(ItemSlot.Kind.ARMOR);
                case ACCESSORIES -> sheet.getSlots(ItemSlot.Kind.ACCESSORY);
            };
        }

    }

    /**
     * What one step does to what its scope names.
     */
    enum Round {

        /**
         * Folds the rows an instance in the slot carries onto that slot's own cells.
         */
        ITEM {

            @Override
            void run(@NotNull StatSheet sheet, @NotNull ConcurrentMap<String, Double> variables, @NotNull ConcurrentList<BuffEvaluator> program, @NotNull Scope scope) {
                scope.slots(sheet).forEach(slot -> applyItemBonuses(slot, variables));
            }

        },

        /**
         * Rescales the profile table by each proportional enchantment substitute an armour piece
         * carries. The one round with no buff grammar in it.
         */
        MULTIPLIER {

            @Override
            void run(@NotNull StatSheet sheet, @NotNull ConcurrentMap<String, Double> variables, @NotNull ConcurrentList<BuffEvaluator> program, @NotNull Scope scope) {
                scope.slots(sheet).forEach(slot -> applyEnchantmentMultipliers(slot.stack(), sheet.getProfile()));
            }

        },

        /**
         * Republishes the totals, so that every expression after this reads what the rounds before it
         * settled rather than what the flat pass left.
         */
        REPUBLISH {

            @Override
            void run(@NotNull StatSheet sheet, @NotNull ConcurrentMap<String, Double> variables, @NotNull ConcurrentList<BuffEvaluator> program, @NotNull Scope scope) {
                sheet.getProfile().publishTotals(variables::put);
                sheet.getProfile().publishOriginTotals(variables::put);
            }

        },

        /**
         * Folds the program's {@code BONUS}-staged rules over the scope's tables.
         */
        PET {

            @Override
            void run(@NotNull StatSheet sheet, @NotNull ConcurrentMap<String, Double> variables, @NotNull ConcurrentList<BuffEvaluator> program, @NotNull Scope scope) {
                fold(sheet, variables, program, scope, Buff.Rule.Stage.BONUS);
            }

        },

        /**
         * Folds the program's {@code POST}-staged rules over the same scopes, after every other round
         * has finished writing.
         */
        POST {

            @Override
            void run(@NotNull StatSheet sheet, @NotNull ConcurrentMap<String, Double> variables, @NotNull ConcurrentList<BuffEvaluator> program, @NotNull Scope scope) {
                fold(sheet, variables, program, scope, Buff.Rule.Stage.POST);
            }

        };

        abstract void run(
            @NotNull StatSheet sheet,
            @NotNull ConcurrentMap<String, Double> variables,
            @NotNull ConcurrentList<BuffEvaluator> program,
            @NotNull Scope scope
        );

    }

    private static void fold(
        @NotNull StatSheet sheet,
        @NotNull ConcurrentMap<String, Double> variables,
        @NotNull ConcurrentList<BuffEvaluator> program,
        @NotNull Scope scope,
        @NotNull Buff.Rule.Stage stage
    ) {
        if (program.isEmpty())
            return;

        if (scope == Scope.PROFILE) {
            BuffEvaluator.Context profileContext = BuffEvaluator.Context.of(variables);
            program.forEach(evaluator -> rewrite(sheet.getProfile(), evaluator, profileContext, stage));
            return;
        }

        scope.slots(sheet).forEach(slot -> {
            BuffEvaluator.Context context = itemContext(slot, variables);
            program.forEach(evaluator -> rewrite(slot.table(), evaluator, context, stage));
        });
    }

    /**
     * Every buff row the summoned pet's perks carry, one evaluator each.
     *
     * <p>
     * The three filters this replaces - a required item, a required mob type, and the percentage flag
     * - are conditions on a row now, so a row that does not apply is <b>counted</b> rather than
     * dropped by a predicate nothing reports.
     */
    private static @NotNull ConcurrentList<BuffEvaluator> petPerkRows(@NotNull ResolvedPet resolvedPet) {
        return resolvedPet.getPerkStats()
            .stream()
            .map(ResolvedPet.PerkStat::perkName)
            .distinct()
            .flatMap(perkName -> BuffEvaluator.select(Buff.Subject.Kind.PET_PERK, resolvedPet.getPet().getId(), perkName).stream())
            .map(BuffEvaluator::compile)
            .collect(Concurrent.toUnmodifiableList());
    }

    /**
     * Applies every row attached to one instance, to that instance's own cells.
     *
     * <p>
     * A row no longer says which bucket it lands in. The three flags that fanned one row into up to
     * three of them are gone with no replacement: nothing could check that a row attached to an item
     * was entitled to write the reforge bucket, and a row that did would break the table's one
     * invariant with nothing anywhere saying so. The bucket is the writer's business, so a carrier
     * rule rewrites what the carrier contributed and the split inside that is not addressable.
     */
    private static void applyItemBonuses(@NotNull StatSheet.Slot slot, @NotNull ConcurrentMap<String, Double> variables) {
        ItemStack itemStats = slot.stack();
        ConcurrentList<Buff> rows = Concurrent.newList(BuffEvaluator.select(Buff.Subject.Kind.ITEM, itemStats.getItem().getId(), null));

        itemStats.getReforge().ifPresent(reforge -> rows.addAll(BuffEvaluator.select(Buff.Subject.Kind.REFORGE, reforge.getId(), null)));
        itemStats.getEnchantments().forEach((enchantment, level) -> rows.addAll(BuffEvaluator.select(Buff.Subject.Kind.ENCHANTMENT, enchantment.getId(), null)));

        if (rows.isEmpty())
            return;

        BuffEvaluator.Context context = itemContext(slot, variables);

        // an item bonus lands on the bonus half only - the base half is what the source itself gave
        rows.stream()
            .map(BuffEvaluator::compile)
            .forEach(evaluator -> slot.table().getEntries().forEach((bucket, statEntries) -> statEntries.forEach((statModel, data) -> StatHalf.BONUS.set(
                data,
                evaluator.apply(statModel, Buff.Channel.VALUE, Buff.Rule.Stage.BONUS, data.getBonus(), context)
            ))));
    }

    private static @NotNull BuffEvaluator.Context itemContext(@NotNull StatSheet.Slot slot, @NotNull ConcurrentMap<String, Double> variables) {
        ItemStack itemStats = slot.stack();

        return BuffEvaluator.Context.of(variables)
            .carrier(Buff.Term.Carrier.ID, itemStats.getItem().getId())
            .carrier(Buff.Term.Carrier.RARITY, itemStats.getRarity().name())
            .carrier(Buff.Term.Carrier.CATEGORY, itemStats.getItem().getCategory().getId())
            .carrier(Buff.Term.Carrier.SLOT, slot.stack().getAccessory().isPresent() ? ItemSlot.Kind.ACCESSORY.name() : ItemSlot.Kind.ARMOR.name())
            .tag(itemStats.getCompoundTag());
    }

    private static void applyEnchantmentMultipliers(@NotNull ItemStack itemStats, @NotNull StatTable table) {
        itemStats.getEnchantments().forEach((enchantment, level) -> itemStats.getEnchantmentStats().get(enchantment)
            .stream()
            .filter(sub -> sub.getStat().isPresent())
            .filter(sub -> sub.getType() == Stat.Type.PERCENT || sub.getType() == Stat.Type.PLUS_PERCENT)
            .forEach(sub -> {
                double enchantMultiplier = 1 + sub.getValues()
                    .entrySet()
                    .stream()
                    .filter(entry -> level >= entry.getKey())
                    .mapToDouble(java.util.Map.Entry::getValue)
                    .sum() / 100.0;

                table.rewrite(sub.getStat().get(), (statModel, half, current) -> current * enchantMultiplier);
            })
        );
    }

    private static void rewrite(@NotNull StatTable table, @NotNull BuffEvaluator evaluator, @NotNull BuffEvaluator.Context context, @NotNull Buff.Rule.Stage stage) {
        table.rewrite((statModel, half, current) -> evaluator.apply(statModel, Buff.Channel.VALUE, stage, current, context));
    }

}
