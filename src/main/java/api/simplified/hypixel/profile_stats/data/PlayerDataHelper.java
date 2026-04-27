package api.simplified.hypixel.profile_stats.data;

import dev.sbs.skyblockdata.common.Rarity;
import dev.sbs.skyblockdata.date.SkyBlockDate;
import dev.sbs.skyblockdata.model.BuffEffectsModel;
import dev.sbs.skyblockdata.model.Gemstone;
import dev.sbs.skyblockdata.model.Reforge;
import dev.sbs.skyblockdata.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.expression.Expression;
import dev.simplified.util.NumberUtil;
import dev.simplified.util.mutable.MutableBoolean;
import dev.simplified.util.mutable.MutableDouble;
import lib.minecraft.nbt.tags.collection.CompoundTag;
import lib.minecraft.nbt.tags.primitive.IntTag;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerDataHelper {

    private static final Pattern nbtVariablePattern = Pattern.compile(".*?(nbt_([a-zA-Z0-9_\\-.]+)).*?");

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

    public static ConcurrentMap<Stat, Double> handleGemstoneBonus(ObjectData<?> objectData) {
        ConcurrentMap<Stat, Double> gemstoneAdjusted = Concurrent.newMap();

        objectData.getGemstones().forEach(entry -> entry.getValue().forEach(gemstoneType -> {
            Gemstone gemstone = entry.getKey();
            double value = gemstone.getValues()
                .getOrDefault(gemstoneType, Concurrent.newMap())
                .getOrDefault(objectData.getRarity(), 0.0);

            if (value > 0.0)
                gemstoneAdjusted.put(gemstone.getStat(), value + gemstoneAdjusted.getOrDefault(gemstone.getStat(), 0.0));
        }));

        return gemstoneAdjusted;
    }

    public static ConcurrentMap<Stat, Double> handleReforgeBonus(@NotNull Optional<Reforge> optionalReforge, @NotNull Rarity rarity) {
        ConcurrentMap<Stat, Double> reforgeBonuses = Concurrent.newMap();

        optionalReforge.ifPresent(reforge -> reforge.getStats(rarity)
            .forEach(substitute -> substitute.getStat()
                .ifPresent(stat -> reforgeBonuses.put(stat, substitute.getValues().get(rarity) + reforgeBonuses.getOrDefault(stat, 0.0)))));

        return reforgeBonuses;
    }

}
