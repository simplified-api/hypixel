package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.skill.Skill;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;

@Getter
public class PlayerData {

    @SerializedName("active_effects")
    private @NotNull ConcurrentList<Potion> activePotions = Concurrent.newList();
    @SerializedName("perks")
    private @NotNull ConcurrentMap<String, Integer> essencePerks = Concurrent.newMap();
    @SerializedName("paused_effects")
    private @NotNull ConcurrentList<Potion> pausedPotions = Concurrent.newList();
    @SerializedName("visited_modes")
    private @NotNull ConcurrentList<String> visitedModes = Concurrent.newList();
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<Skill.Type, Double> experience = Concurrent.newMap();
    @SerializedName("death_count")
    private int deathCount;
    @SerializedName("last_death")
    private SkyBlockDate.SkyBlockTime lastDeath;
    @SerializedName("unlocked_coll_tiers")
    private @NotNull ConcurrentList<String> unlockedCollectionTiers = Concurrent.newList();
    @SerializedName("visited_zones")
    private @NotNull ConcurrentList<String> visitedZones = Concurrent.newList();
    @SerializedName("disabled_potion_effects")
    private @NotNull ConcurrentList<String> disabledPotions = Concurrent.newList();
    @SerializedName("temp_stat_buffs")
    private @NotNull ConcurrentList<CenturyCake> centuryCakes = Concurrent.newList();
    @SerializedName("reaper_peppers_eaten")
    private boolean reaperPeppersEaten;
    @SerializedName("crafted_generators")
    private @NotNull ConcurrentList<String> craftedMinions = Concurrent.newList();
    @SerializedName("fishing_treasure_caught")
    private int fishingTreasureCaught;
    @SerializedName("achievement_spawned_island_types")
    private @NotNull ConcurrentList<String> spawnedIslandTypes = Concurrent.newList();

    public @NotNull ConcurrentList<Integer> getCraftedMinions(@NotNull String itemId) {
        return this.getCraftedMinions()
            .stream()
            .filter(item -> item.matches(String.format("^%s_[\\d]+$", itemId)))
            .map(item -> Integer.parseInt(item.replace(String.format("%s_", itemId), "")))
            .collect(Concurrent.toList())
            .sorted(Comparator.naturalOrder());
    }

    public @NotNull Skill getSkill(@NotNull Skill.Type skillType) {
        return new Skill(skillType, this.experience.get(skillType));
    }

    public @NotNull ConcurrentList<Skill> getSkills() {
        return this.getSkills(true);
    }

    public @NotNull ConcurrentList<Skill> getSkills(boolean includeCosmetic) {
        return Arrays.stream(Skill.Type.values())
            .filter(type -> includeCosmetic || !type.isCosmetic())
            .map(this::getSkill)
            .collect(Concurrent.toList());
    }

    @Getter
    public static class CenturyCake {

        private int stat; // This is in ordinal order in stat menu
        private String key;
        private int amount;
        @SerializedName("expire_at")
        private SkyBlockDate.RealTime expiresAt;

    }

}
