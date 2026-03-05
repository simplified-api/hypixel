package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.Model;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.common.Rarity;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface Pet extends Model {

    @NotNull ConcurrentList<Integer> PET_SCORE = Concurrent.newList(
        10, 25, 50, 75, 100, 130, 175,
        225, 275, 325, 375, 450, 500
    );

    @NotNull String getId();

    @NotNull String getName();

    @NotNull Rarity getLowestRarity();

    @NotNull String getSkillId();

    default @NotNull Skill getSkill() {
        return MinecraftApi.getRepository(Skill.class)
            .findFirstOrNull(Skill::getId, this.getSkillId());
    }

    @NotNull Type getType();

    int getMaxLevel();

    boolean isPassive();

    @NotNull ConcurrentList<Substitute> getStats();

    default @NotNull ConcurrentList<Substitute> getStats(@NotNull Rarity rarity) {
        return this.getStats()
            .stream()
            .filter(stat -> stat.getValues().containsKey(rarity))
            .collect(Concurrent.toUnmodifiableList());
    }

    @NotNull ConcurrentList<Ability> getAbilities();

    default @NotNull ConcurrentList<Ability> getAbilities(@NotNull Rarity rarity) {
        return this.getAbilities()
            .stream()
            .filter(ability -> ability.getStats()
                .stream()
                .anyMatch(stat -> stat.getValues().containsKey(rarity))
            )
            .collect(Concurrent.toUnmodifiableList());
    }

    interface Ability {

        @NotNull String getName();

        @NotNull String getDescription();

        default @NotNull String getFormattedDescription() {
            // TODO: Populate STAT:X, VALUE:X
            //  Perform a StringUtil.format on description
            //  given the stats
            return this.getDescription();
        }

        boolean isFlatStat();

        @NotNull ConcurrentList<Substitute> getStats();

        default @NotNull ConcurrentList<Substitute> getStats(@NotNull Rarity rarity) {
            return this.getStats()
                .stream()
                .filter(stat -> stat.getValues().containsKey(rarity))
                .collect(Concurrent.toUnmodifiableList());
        }

    }

    interface Substitute {

        @NotNull String getId();

        default @NotNull Optional<Stat> getStat() {
            return MinecraftApi.getRepository(Stat.class)
                .findFirst(Stat::getId, this.getId());
        }

        int getPrecision();

        @NotNull Stat.Type getType();

        @NotNull ChatFormat getFormat();

        @NotNull ConcurrentMap<Rarity, Value> getValues();

        interface Value {

            double getBase();

            double getScalar();

        }

    }

    enum Type {

        PET,
        MOUNT,
        MORPH,
        GABAGOOL;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

    }

}
