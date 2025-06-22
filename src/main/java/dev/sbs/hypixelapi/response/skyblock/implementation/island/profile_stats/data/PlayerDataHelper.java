package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.profile_stats.data;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.data.model.BuffEffectsModel;
import dev.sbs.minecraftapi.data.model.gemstone_data.gemstone_stats.GemstoneStatModel;
import dev.sbs.minecraftapi.data.model.reforge_data.reforge_stats.ReforgeStatModel;
import dev.sbs.minecraftapi.data.model.stats.StatModel;
import dev.sbs.api.math.Expression;
import dev.sbs.api.math.ExpressionBuilder;
import dev.sbs.api.mutable.MutableBoolean;
import dev.sbs.api.mutable.MutableDouble;
import dev.sbs.api.stream.pair.Pair;
import dev.sbs.api.stream.triple.Triple;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.IntTag;
import dev.sbs.minecraftapi.util.SkyBlockDate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerDataHelper {

    private static final Pattern nbtVariablePattern = Pattern.compile(".*?(nbt_([a-zA-Z0-9_\\-.]+)).*?");

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static double handlePostBonusEffects(StatModel statModel, double currentTotal, CompoundTag compoundTag, Map<String, Double> variables, BuffEffectsModel... bonusEffectsModels) {
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

                if (statModel.getKey().equals(filterKey) || filterKey.endsWith("ALL")) {
                    String valueString = String.valueOf(buffValue);
                    valueString = valueString.replace("PET_ALL", String.format("PET_%s", statModel.getKey()));

                    if (compoundTag != null) {
                        Matcher nbtMatcher = nbtVariablePattern.matcher(valueString);

                        if (nbtMatcher.matches()) {
                            String nbtTag = nbtMatcher.group(2);
                            String nbtValue = String.valueOf(compoundTag.getPathOrDefault(nbtTag, IntTag.EMPTY).getValue());
                            valueString = valueString.replace(nbtMatcher.group(1), nbtValue);
                        }
                    }

                    if (copy) {
                        Expression expression = new ExpressionBuilder(String.format("%s * (%s)", currentTotal, valueString))
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static double handleBonusEffects(StatModel statModel, double currentTotal, CompoundTag compoundTag, Map<String, Double> variables, BuffEffectsModel... bonusEffectsModels) {
        MutableDouble value = new MutableDouble(currentTotal);

        for (BuffEffectsModel bonusEffectModel : bonusEffectsModels) {
            value.add((double) bonusEffectModel.getEffect(statModel.getKey(), 0.0));

            bonusEffectModel.getBuffEffects().forEach((buffKey, buffValue) -> {
                String filterKey = (String) buffKey;

                if (filterKey.equals("TIME")) {
                    SkyBlockDate currentDate = new SkyBlockDate(System.currentTimeMillis());
                    int hour = currentDate.getHour();
                    List<String> timeConstraints = (List<String>) buffValue;
                    MutableBoolean insideConstraint = new MutableBoolean(false);

                    timeConstraints.forEach(timeConstraint -> {
                        String[] constraintParts = timeConstraint.split("-");
                        int start = NumberUtil.toInt(constraintParts[0]);
                        int end = NumberUtil.toInt(constraintParts[1]);

                        if (hour >= start && hour <= end)
                            insideConstraint.setTrue(); // At Least 1 Constraint is True
                    });

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
                    if (statModel.getKey().equals(filterKey) || "ALL".equals(filterKey)) {
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

                                Expression expression = new ExpressionBuilder(String.format("%s %s (%s)", currentTotal, (multiply ? "*" : "+"), valueString))
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

    public static ConcurrentMap<StatModel, Double> handleGemstoneBonus(ObjectData<?> objectData) {
        ConcurrentMap<StatModel, Double> gemstoneAdjusted = Concurrent.newMap();

        objectData.getGemstones().forEach(entry -> entry.getValue()
            .stream()
            .map(gemstoneTypeModel -> Triple.of(
                entry.getKey(),
                gemstoneTypeModel,
                SimplifiedApi.getRepositoryOf(GemstoneStatModel.class).findFirst(
                    Pair.of(GemstoneStatModel::getGemstone, entry.getKey()),
                    Pair.of(GemstoneStatModel::getType, gemstoneTypeModel),
                    Pair.of(GemstoneStatModel::getRarity, objectData.getRarity())
                )
            ))
            .filter(gemstoneData -> gemstoneData.getRight().isPresent())
            .forEach(gemstoneData -> gemstoneAdjusted.put(
                gemstoneData.getLeft().getStat(),
                gemstoneData.getRight().get().getValue() + gemstoneAdjusted.getOrDefault(gemstoneData.getLeft().getStat(), 0.0))
            )
        );

        return gemstoneAdjusted;
    }

    public static ConcurrentMap<StatModel, Double> handleReforgeBonus(Optional<ReforgeStatModel> optionalReforgeStatModel) {
        ConcurrentMap<StatModel, Double> reforgeBonuses = Concurrent.newMap();

        // Load Reforge Stat Effects
        optionalReforgeStatModel.ifPresent(reforgeStatModel -> reforgeStatModel.getEffects()
            .forEach((key, value) -> SimplifiedApi.getRepositoryOf(StatModel.class).findFirst(StatModel::getKey, key)
                .ifPresent(statModel -> reforgeBonuses.put(statModel, value + reforgeBonuses.getOrDefault(statModel, 0.0)))));

        return reforgeBonuses;
    }

}
