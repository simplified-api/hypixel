package api.simplified.hypixel.profile_stats.data;

import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.BuffEffectsModel;
import api.simplified.skyblock.model.Gemstone;
import api.simplified.skyblock.model.Reforge;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.expression.Expression;
import dev.simplified.util.NumberUtil;
import dev.simplified.util.mutable.MutableBoolean;
import dev.simplified.util.mutable.MutableDouble;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.IntTag;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluation of the stat bonuses that reference data declares as expressions rather than as numbers.
 * <p>
 * A great many SkyBlock bonuses are conditional - they scale on a stat the player already has, on a
 * value stored in the item's own tag, or on the time of day. Reference data therefore stores them as
 * expression strings keyed by a prefixed stat id, and nothing can be resolved until both the player's
 * variables and the item's tag are in hand. That is what this does.
 * <p>
 * The key prefixes carry the operation: {@code ADD_} and {@code MULTIPLY_} combine with the running
 * total, {@code COPY_} scales it, {@code SET_} replaces it, and {@code ALL} in place of a stat id
 * applies the bonus to every stat. A value may embed {@code nbt_<path>} to read a number straight
 * out of the item's tag, and {@code CURRENT_VALUE} to refer to the running total.
 */
public class PlayerDataHelper {

    private static final Pattern nbtVariablePattern = Pattern.compile(".*?(nbt_([a-zA-Z0-9_\\-.]+)).*?");

    /**
     * Applies the buffs that have to run after every other source has been totalled.
     * <p>
     * Only the {@code COPY_} form does anything here, since a bonus that scales the finished total
     * cannot be evaluated while that total is still being assembled.
     *
     * @param statModel the stat being totalled
     * @param currentTotal the value every other source has already produced
     * @param compoundTag the item's tag, read for any {@code nbt_} reference, may be null
     * @param variables the player state the expressions are evaluated against
     * @param bonusEffectsModels the reference data declaring the buffs
     * @return the adjusted total
     */
    public static double handlePostBonusEffects(Stat statModel, double currentTotal, CompoundTag compoundTag, Map<String, Double> variables, BuffEffectsModel... bonusEffectsModels) {
        MutableDouble value = new MutableDouble(currentTotal);

        Arrays.stream(bonusEffectsModels).forEach(bonusEffectsModel -> bonusEffectsModel.getBuffEffects()
            .forEach((buffKey, buffValue) -> {
                String filterKey = (String) buffKey;
                boolean copy = false;

                if (filterKey.startsWith("COPY_")) {
                    filterKey = filterKey.replace("COPY_", "");
                    copy = true;
                } else if (filterKey.startsWith("SET_"))
                    filterKey = filterKey.replace("SET_", "");

                if (statModel.getId().equals(filterKey) || filterKey.endsWith("ALL")) {
                    String valueString = String.valueOf(buffValue);
                    valueString = valueString.replace("PET_ALL", String.format("PET_%s", statModel.getId()));

                    if (compoundTag != null) {
                        Matcher nbtMatcher = nbtVariablePattern.matcher(valueString);

                        if (nbtMatcher.matches()) {
                            String nbtTag = nbtMatcher.group(2);
                            String nbtValue = String.valueOf(compoundTag.getPathOrDefault(nbtTag, IntTag.EMPTY).getValue());
                            valueString = valueString.replace(nbtMatcher.group(1), nbtValue);
                        }
                    }

                    if (copy) {
                        Expression expression = Expression.builder(String.format("%s * (%s)", currentTotal, valueString))
                            .variables(variables.keySet())
                            .build()
                            .setVariables(variables)
                            .setVariable("CURRENT_VALUE", currentTotal);

                        value.set(expression.evaluate());
                    } else {
                        // TODO: HMMM???
                    }
                }
            })
        );

        return value.get();
    }

    /**
     * Applies the buffs a source declares, folding each into the running total.
     * <p>
     * A {@code TIME} key is a list of hour ranges the buff holds within; falling outside every one of
     * them zeroes the value rather than skipping it. A multiplying buff is ignored for a stat that
     * cannot be multiplied.
     *
     * @param statModel the stat being totalled
     * @param currentTotal the value accumulated so far
     * @param compoundTag the item's tag, read for any {@code nbt_} reference, may be null
     * @param variables the player state the expressions are evaluated against
     * @param bonusEffectsModels the reference data declaring the buffs
     * @return the adjusted total
     */
    @SuppressWarnings("unchecked")
    public static double handleBonusEffects(
        Stat statModel,
        double currentTotal,
        CompoundTag compoundTag,
        Map<String, Double> variables,
        BuffEffectsModel... bonusEffectsModels
    ) {
        MutableDouble value = new MutableDouble(currentTotal);

        for (BuffEffectsModel bonusEffectModel : bonusEffectsModels) {
            value.add(bonusEffectModel.getEffect(statModel.getId(), 0.0));

            bonusEffectModel.getBuffEffects().forEach((buffKey, buffValue) -> {
                String filterKey = buffKey;

                if (filterKey.equals("TIME")) {
                    SkyBlockDate currentDate = new SkyBlockDate(System.currentTimeMillis());
                    MutableBoolean insideConstraint = getMutableBoolean((List<String>) buffValue, currentDate);

                    if (insideConstraint.isFalse())
                        value.set(0.0);
                } else {
                    boolean multiply = false;

                    if (filterKey.startsWith("MULTIPLY_")) {
                        filterKey = filterKey.replace("MULTIPLY_", "");
                        multiply = true;
                    } else if (filterKey.startsWith("ADD_"))
                        filterKey = filterKey.replace("ADD_", "");

                    // Handle Buff Stat
                    if (statModel.getId().equals(filterKey) || "ALL".equals(filterKey)) {
                        String valueString = String.valueOf(buffValue);

                        if (NumberUtil.isCreatable(valueString))
                            value.set(value.get() * (double) buffValue);
                        else {
                            if (!multiply || statModel.isMultiplicable()) {
                                if (compoundTag != null) {
                                    Matcher nbtMatcher = nbtVariablePattern.matcher(valueString);

                                    if (nbtMatcher.matches()) {
                                        String nbtTag = nbtMatcher.group(2);
                                        String nbtValue = String.valueOf(compoundTag.getPathOrDefault(nbtTag, IntTag.EMPTY).getValue());
                                        valueString = valueString.replace(nbtMatcher.group(1), nbtValue);
                                    }
                                }

                                Expression expression = Expression.builder(String.format("%s %s (%s)", currentTotal, (multiply ? "*" : "+"), valueString))
                                    .variables(variables.keySet())
                                    .build()
                                    .setVariables(variables)
                                    .setVariable("CURRENT_VALUE", currentTotal);

                                value.set(expression.evaluate());
                            }
                        }
                    }
                }
            });
        }

        return value.get();
    }

    private static @NotNull MutableBoolean getMutableBoolean(List<String> buffValue, SkyBlockDate currentDate) {
        int hour = currentDate.getHour();
        MutableBoolean insideConstraint = new MutableBoolean(false);

        buffValue.forEach(timeConstraint -> {
            String[] constraintParts = timeConstraint.split("-");
            int start = NumberUtil.toInt(constraintParts[0]);
            int end = NumberUtil.toInt(constraintParts[1]);

            if (hour >= start && hour <= end)
                insideConstraint.setTrue(); // At Least 1 Constraint is True
        });

        return insideConstraint;
    }

    /**
     * Totals what a set of slotted gemstones give, at the rarity of what they are slotted into.
     * <p>
     * A gemstone's value depends on both its quality and the rarity of what it is slotted into, so
     * the same gemstone is worth more in a legendary item than in a rare one.
     *
     * @param gemstones the quality of each slotted gemstone, keyed by the gemstone
     * @param rarity the rarity the gemstones are scaled against
     * @return the value each stat gains, keyed by the stat
     */
    public static ConcurrentMap<Stat, Double> handleGemstoneBonus(@NotNull ConcurrentMap<Gemstone, ConcurrentList<Gemstone.Type>> gemstones, @NotNull Rarity rarity) {
        ConcurrentMap<Stat, Double> gemstoneAdjusted = Concurrent.newMap();

        gemstones.forEach((gemstone, gemstoneTypes) -> gemstoneTypes.forEach(gemstoneType -> {
            double value = gemstone.getValues()
                .getOrDefault(gemstoneType, Concurrent.newMap())
                .getOrDefault(rarity, 0.0);

            if (value > 0.0)
                gemstoneAdjusted.put(gemstone.getStat(), value + gemstoneAdjusted.getOrDefault(gemstone.getStat(), 0.0));
        }));

        return gemstoneAdjusted;
    }

    /**
     * Totals what a reforge gives at a given rarity.
     *
     * @param optionalReforge the applied reforge, empty when none is
     * @param rarity the rarity the reforge is scaled against
     * @return the value each stat gains, empty when no reforge is applied
     */
    public static ConcurrentMap<Stat, Double> handleReforgeBonus(@NotNull Optional<Reforge> optionalReforge, @NotNull Rarity rarity) {
        ConcurrentMap<Stat, Double> reforgeBonuses = Concurrent.newMap();

        optionalReforge.ifPresent(reforge -> reforge.getStats(rarity)
            .forEach(substitute -> substitute.getStat()
                .ifPresent(stat -> reforgeBonuses.put(stat, substitute.getValues().get(rarity) + reforgeBonuses.getOrDefault(stat, 0.0)))));

        return reforgeBonuses;
    }

}
