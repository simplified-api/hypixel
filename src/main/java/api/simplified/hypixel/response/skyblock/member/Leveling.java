package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * A member's SkyBlock level and everything feeding it.
 *
 * <p>
 * One level is earned per hundred SkyBlock experience, and that experience comes from one-time
 * tasks. Emblems are cosmetic markers unlocked by particular tasks and shown beside the player's
 * name.
 *
 * <p>
 * Only the level itself is derived; every other member is bound, and nothing here reaches a
 * repository.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/SkyBlock_Levels">SkyBlock Levels</a>
 */
@Getter
public class Leveling {

    /**
     * SkyBlock experience earned.
     */
    private int experience;

    /**
     * Repeatable tasks and how many times each has been done.
     */
    private @NotNull ConcurrentMap<String, Integer> completions = Concurrent.newMap();

    /**
     * Finished task ids - the shorter of the two finished-task lists the wire carries.
     */
    private @NotNull ConcurrentList<String> completed = Concurrent.newList();

    /**
     * Hypixel-side bookkeeping flag for a data migration.
     */
    @Accessors(fluent = true)
    @SerializedName("migrated_completions")
    private boolean hasMigratedCompletions;

    /**
     * Menu state for whether the task categories are shown expanded.
     */
    @SerializedName("category_expanded")
    private boolean categoryExpanded;

    /**
     * Task ids the level menu last showed.
     */
    @SerializedName("last_viewed_tasks")
    private @NotNull ConcurrentList<String> lastViewedTasks = Concurrent.newList();

    /**
     * Finished task ids - the fuller of the two finished-task lists the wire carries, and typically
     * far longer than the other.
     */
    @SerializedName("completed_tasks")
    private @NotNull ConcurrentList<String> completedTasks = Concurrent.newList();

    /**
     * Best pet score the member has reached.
     */
    @SerializedName("highest_pet_score")
    private int highestPetScore;

    /**
     * Ores mined during Mining Fiestas.
     */
    @SerializedName("mining_fiesta_ores_mined")
    private int miningFiestaOresMined;

    /**
     * A second Hypixel-side bookkeeping flag for a data migration.
     */
    private boolean migrated;

    /**
     * A third Hypixel-side bookkeeping flag for a data migration.
     */
    @Accessors(fluent = true)
    @SerializedName("migrated_completions_2")
    private boolean hasMigratedCompletions2;

    /**
     * Whether the accessory rewarded for levelling has been taken.
     */
    @Accessors(fluent = true)
    @SerializedName("claimed_talisman")
    private boolean hasClaimedTalisman;

    /**
     * The bonus the member has selected. It carries no default, so an absent key leaves it null.
     */
    @SerializedName("bop_bonus")
    private String bopBonus;

    /**
     * The emblem shown beside the member's name. It carries no default, so an absent key leaves it
     * null - and the key is routinely absent.
     */
    @SerializedName("selected_symbol")
    private String selectedSymbol;

    /**
     * Emblems the member has unlocked.
     */
    @SerializedName("emblem_unlocks")
    private @NotNull ConcurrentList<String> emblemUnlocks = Concurrent.newList();

    /**
     * Sharks killed during Fishing Festivals.
     */
    @SerializedName("fishing_festival_sharks_killed")
    private int fishingFestivalSharksKilled;

    /**
     * SkyBlock level, one per hundred experience.
     *
     * <p>
     * Derived, and always a whole number despite the type - the floor happens inside.
     */
    public double getLevel() {
        return Math.floor(this.getExperience() / 100.0);
    }

}
