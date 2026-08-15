package api.simplified.hypixel.response.skyblock.member;

import api.simplified.hypixel.common.IdTiers;
import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * The member state Hypixel keeps loose rather than under any one feature's node.
 * <p>
 * Two things here are read far more than the rest. The skill experience is the only place skill
 * levels come from - there is no skills node on the wire - and the unlocked collection tiers are a
 * flat list of {@code <id>_<tier>} strings rather than a map, which is why reading a collection's
 * tier means grouping them first.
 */
@Getter
public class PlayerData {

    /**
     * Times the member has died on this profile.
     */
    @SerializedName("death_count")
    private int deathCount;

    /**
     * When the member last died, null for a member who never has.
     */
    @SerializedName("last_death")
    private SkyBlockDate.SkyBlockTime lastDeath;

    /**
     * Fishing treasures the member has caught.
     */
    @SerializedName("fishing_treasure_caught")
    private int fishingTreasureCaught;

    /**
     * Experience earned in each skill, keyed by the skill id, and the only source of skill levels.
     */
    @SerializedName("experience")
    private @NotNull ConcurrentMap<String, Double> skillExperience = Concurrent.newMap();

    // Unlockables

    /**
     * Reaper peppers eaten, each granting a permanent health bonus.
     */
    @SerializedName("reaper_peppers_eaten")
    private int reaperPeppersEaten;

    /**
     * Level bought for each community shop perk, keyed by the perk id.
     */
    @SerializedName("perks")
    private @NotNull ConcurrentMap<String, Integer> shopPerks = Concurrent.newMap();

    /**
     * Garden chips the member holds, keyed by the chip id.
     */
    @SerializedName("garden_chips")
    private @NotNull ConcurrentMap<String, Integer> gardenChips = Concurrent.newMap();

    /**
     * Collection tiers unlocked, each spelled {@code <collection id>_<tier>} in one flat list.
     * <p>
     * A collection id may itself carry underscores and colons, so an entry splits at its <em>last</em>
     * underscore. A tier of {@code -1} marks a collection visible with nothing claimed.
     */
    @SerializedName("unlocked_coll_tiers")
    private @NotNull ConcurrentList<String> unlockedCollectionTiers = Concurrent.newList();

    /**
     * Minions crafted, each spelled {@code <minion id>_<tier>} in one flat list.
     */
    @SerializedName("crafted_generators")
    private @NotNull ConcurrentList<String> craftedMinions = Concurrent.newList();

    // Visited Locations

    /**
     * Ids of the game modes the member has played.
     */
    @SerializedName("visited_modes")
    private @NotNull ConcurrentList<String> visitedModes = Concurrent.newList();

    /**
     * Ids of the zones the member has set foot in.
     */
    @SerializedName("visited_zones")
    private @NotNull ConcurrentList<String> visitedZones = Concurrent.newList();

    /**
     * Ids of the island types the member has spawned, tracked for an achievement.
     */
    @SerializedName("achievement_spawned_island_types")
    private @NotNull ConcurrentList<String> spawnedIslandTypes = Concurrent.newList();

    // Potions & Buffs

    /**
     * Potion effects currently running.
     */
    @SerializedName("active_effects")
    private @NotNull ConcurrentList<PotionData> activePotions = Concurrent.newList();

    /**
     * Potion effects paused, which keep their remaining duration until resumed.
     */
    @SerializedName("paused_effects")
    private @NotNull ConcurrentList<PotionData> pausedPotions = Concurrent.newList();

    /**
     * Ids of the potion effects the member has switched off.
     */
    @SerializedName("disabled_potion_effects")
    private @NotNull ConcurrentList<String> disabledPotions = Concurrent.newList();

    /**
     * Century cakes in effect, each granting its stat until it expires.
     */
    @SerializedName("temp_stat_buffs")
    private @NotNull ConcurrentList<CenturyCake> centuryCakes = Concurrent.newList();

    /**
     * Reads the tiers crafted for one minion type, lowest first.
     *
     * @param itemId the minion's item id
     * @return the tiers crafted, empty when the minion has never been crafted
     */
    public @NotNull ConcurrentList<Integer> getCraftedMinions(@NotNull String itemId) {
        return IdTiers.group(this.getCraftedMinions())
            .getOrDefault(itemId, Concurrent.newList())
            .sorted(Comparator.naturalOrder());
    }

}
