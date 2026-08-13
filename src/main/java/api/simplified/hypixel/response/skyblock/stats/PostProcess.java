package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.skyblock.model.BonusItemStat;
import api.simplified.skyblock.model.BonusPetPerkStat;
import api.simplified.skyblock.model.BuffEffectsModel;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

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
     * @param variables the expression variables, republished as each round settles
     * @param table the profile's own table
     * @param armor the armour pieces, absent slots included
     * @param accessories the accessories that survived the family filter
     */
    public static void run(
        @NotNull StatContext context,
        @NotNull ConcurrentMap<String, Double> variables,
        @NotNull StatTable table,
        @NotNull ConcurrentList<Optional<ItemStack>> armor,
        @NotNull ConcurrentList<ItemStack> accessories
    ) {
        ConcurrentList<BonusPetPerkStat> bonusPerkStats = ResolvedPet.of(context.getMember())
            .map(ResolvedPet::getBonusPerkStats)
            .orElseGet(Concurrent::newUnmodifiableList);

        // --- Accessory Bonus ---
        accessories.forEach(accessoryStats -> applyItemBonuses(accessoryStats, variables));

        // --- Armour Bonus ---
        armor.stream()
            .flatMap(Optional::stream)
            .forEach(itemStats -> applyItemBonuses(itemStats, variables));

        // --- Enchantment Multiplier ---
        armor.stream()
            .flatMap(Optional::stream)
            .forEach(itemStats -> applyEnchantmentMultipliers(itemStats, table));

        // --- Pet Static ---
        // a rule that reads a total belongs where the totals are settled, not part-way through the
        // pass that is still writing them
        table.publishTotals(variables::put);
        bonusPerkStats.stream()
            .filter(BonusPetPerkStat::notPercentage)
            .filter(BonusPetPerkStat::noRequiredItem)
            .filter(BonusPetPerkStat::noRequiredMobType)
            .forEach(bonusPetPerkStat -> rewrite(
                table,
                BuffEvaluator.compile(bonusPetPerkStat).bind(null),
                variables
            ));

        // --- Pet Percentage ---
        table.publishTotals(variables::put);
        bonusPerkStats.stream()
            .filter(BonusPetPerkStat::isPercentage)
            .filter(BonusPetPerkStat::noRequiredItem)
            .filter(BonusPetPerkStat::noRequiredMobType)
            .forEach(bonusPetPerkStat -> {
                BuffEvaluator evaluator = BuffEvaluator.compile(bonusPetPerkStat);

                // profile, then armour, then accessories - a pet percentage reads a total the level
                // above it has already settled
                rewrite(table, evaluator.bind(null), variables);

                armor.stream()
                    .flatMap(Optional::stream)
                    .forEach(itemStats -> rewrite(itemStats.getTable(), evaluator.bind(itemStats.getCompoundTag()), variables));

                accessories.forEach(accessoryStats -> rewrite(accessoryStats.getTable(), evaluator.bind(accessoryStats.getCompoundTag()), variables));
            });
    }

    private static void applyItemBonuses(@NotNull ItemStack itemStats, @NotNull ConcurrentMap<String, Double> variables) {
        // Handle Reforges
        itemStats.getBonusReforgeStatModel().ifPresent(bonusReforgeStat -> rewriteBucket(itemStats, ItemOrigin.REFORGES, bonusReforgeStat, variables));

        // Handle Bonus Item Stats
        itemStats.getBonusItemStatModels()
            .stream()
            .filter(BonusItemStat::noRequiredMobType)
            .forEach(bonusItemStat -> {
                // Handle Bonus Gemstone Stats
                if (bonusItemStat.isForGems())
                    rewriteBucket(itemStats, ItemOrigin.GEMSTONES, bonusItemStat, variables);

                // Handle Bonus Reforges
                if (bonusItemStat.isForReforges())
                    rewriteBucket(itemStats, ItemOrigin.REFORGES, bonusItemStat, variables);

                // Handle Bonus Stats
                if (bonusItemStat.isForStats())
                    rewriteBucket(itemStats, ItemOrigin.STATS, bonusItemStat, variables);
            });
    }

    private static void rewriteBucket(@NotNull ItemStack itemStats, @NotNull ItemOrigin bucket, @NotNull BuffEffectsModel model, @NotNull ConcurrentMap<String, Double> variables) {
        BuffEvaluator evaluator = BuffEvaluator.compile(model).bind(itemStats.getCompoundTag());

        // an item bonus lands on the bonus half only - the base half is what the source itself gave
        itemStats.getStats(bucket).forEach((statModel, data) -> StatHalf.BONUS.set(
            data,
            evaluator.apply(statModel, data.getBonus(), variables, Operation.Pass.BONUS)
        ));
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

    private static void rewrite(@NotNull StatTable table, @NotNull BuffEvaluator evaluator, @NotNull ConcurrentMap<String, Double> variables) {
        table.rewrite((statModel, half, current) -> evaluator.apply(statModel, current, variables, Operation.Pass.BONUS));
    }

}
