package api.simplified.hypixel.response.skyblock.member;

import api.simplified.hypixel.common.IdTiers;
import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

@Getter
public class PlayerData {

    @SerializedName("death_count")
    private int deathCount;
    @SerializedName("last_death")
    private SkyBlockDate.SkyBlockTime lastDeath;
    @SerializedName("fishing_treasure_caught")
    private int fishingTreasureCaught;
    @SerializedName("experience")
    private @NotNull ConcurrentMap<String, Double> skillExperience = Concurrent.newMap();

    // Unlockables
    @SerializedName("reaper_peppers_eaten")
    private int reaperPeppersEaten;
    @SerializedName("perks")
    private @NotNull ConcurrentMap<String, Integer> shopPerks = Concurrent.newMap();
    @SerializedName("garden_chips")
    private @NotNull ConcurrentMap<String, Integer> gardenChips = Concurrent.newMap();
    @SerializedName("unlocked_coll_tiers")
    private @NotNull ConcurrentList<String> unlockedCollectionTiers = Concurrent.newList();
    @SerializedName("crafted_generators")
    private @NotNull ConcurrentList<String> craftedMinions = Concurrent.newList();

    // Visited Locations
    @SerializedName("visited_modes")
    private @NotNull ConcurrentList<String> visitedModes = Concurrent.newList();
    @SerializedName("visited_zones")
    private @NotNull ConcurrentList<String> visitedZones = Concurrent.newList();
    @SerializedName("achievement_spawned_island_types")
    private @NotNull ConcurrentList<String> spawnedIslandTypes = Concurrent.newList();

    // Potions & Buffs
    @SerializedName("active_effects")
    private @NotNull ConcurrentList<PotionData> activePotions = Concurrent.newList();
    @SerializedName("paused_effects")
    private @NotNull ConcurrentList<PotionData> pausedPotions = Concurrent.newList();
    @SerializedName("disabled_potion_effects")
    private @NotNull ConcurrentList<String> disabledPotions = Concurrent.newList();
    @SerializedName("temp_stat_buffs")
    private @NotNull ConcurrentList<CenturyCake> centuryCakes = Concurrent.newList();

    public @NotNull ConcurrentList<Integer> getCraftedMinions(@NotNull String itemId) {
        return IdTiers.group(this.getCraftedMinions())
            .getOrDefault(itemId, Concurrent.newList())
            .sorted(Comparator.naturalOrder());
    }

}
