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
 * Four sub-steps, and the order between them is the whole of why this is a type rather than four
 * loops. The enchantment multiplier rescales what the item bonuses have already added, so it runs
 * after them; the pet percentage rescales what the multiplier has already scaled, so it runs after
 * that. Only a reference row can make the order visible, and the six tables that would carry one
 * ship no rows, so the order is held in place by the tests rather than by the corpus.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PostProcess {

    /**
     * Runs the four sub-steps, in the order the totals they read require.
     *
     * @param context the member and the island to read from
     * @param sheet the profile's own table and one per filled item slot
     * @param variables the expression variables, republished as each round settles
     */
    public static void run(
        @NotNull StatContext context,
        @NotNull StatSheet sheet,
        @NotNull ConcurrentMap<String, Double> variables
    ) {
        StatTable table = sheet.getProfile();
        ConcurrentList<StatSheet.Slot> armor = sheet.getSlots(ItemSlot.Kind.ARMOR);
        ConcurrentList<StatSheet.Slot> accessories = sheet.getSlots(ItemSlot.Kind.ACCESSORY);

        ConcurrentList<BuffEvaluator> petPerks = ResolvedPet.of(context.getMember())
            .map(PostProcess::petPerkRows)
            .orElseGet(Concurrent::newUnmodifiableList);

        // --- Accessory Bonus ---
        accessories.forEach(slot -> applyItemBonuses(slot, variables));

        // --- Armour Bonus ---
        armor.forEach(slot -> applyItemBonuses(slot, variables));

        // --- Enchantment Multiplier ---
        armor.forEach(slot -> applyEnchantmentMultipliers(slot.stack(), table));

        // --- Pet Rules ---
        // a rule that reads a total belongs where the totals are settled, not part-way through the
        // pass that is still writing them. What was two steps split by a percentage flag is one, and
        // the operation rank is what orders the flat contributions ahead of the proportional ones
        table.publishTotals(variables::put);
        table.publishOriginTotals(variables::put);

        BuffEvaluator.Context profileContext = BuffEvaluator.Context.of(variables);

        // profile, then armour, then accessories - a pet rule reads a total the level above it has
        // already settled
        petPerks.forEach(evaluator -> {
            rewrite(table, evaluator, profileContext);
            armor.forEach(slot -> rewrite(slot.table(), evaluator, itemContext(slot, variables)));
            accessories.forEach(slot -> rewrite(slot.table(), evaluator, itemContext(slot, variables)));
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

    private static void rewrite(@NotNull StatTable table, @NotNull BuffEvaluator evaluator, @NotNull BuffEvaluator.Context context) {
        table.rewrite((statModel, half, current) -> evaluator.apply(statModel, Buff.Channel.VALUE, Buff.Rule.Stage.BONUS, current, context));
    }

}
