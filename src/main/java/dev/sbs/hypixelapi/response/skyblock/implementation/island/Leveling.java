package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Getter
public class Leveling {

    private int experience;
    private @NotNull ConcurrentMap<String, Integer> completions = Concurrent.newMap();
    private @NotNull ConcurrentList<String> completed = Concurrent.newList();
    @Accessors(fluent = true)
    @SerializedName("migrated_completions")
    private boolean hasMigratedCompletions;
    @SerializedName("category_expanded")
    private boolean categoryExpanded;
    @SerializedName("last_viewed_tasks")
    private @NotNull ConcurrentList<String> lastViewedTasks = Concurrent.newList();
    @SerializedName("completed_tasks")
    private @NotNull ConcurrentList<String> completedTasks = Concurrent.newList();
    @SerializedName("highest_pet_score")
    private int highestPetScore;
    @SerializedName("mining_fiesta_ores_mined")
    private int miningFiestaOresMined;
    private boolean migrated;
    @Accessors(fluent = true)
    @SerializedName("migrated_completions_2")
    private boolean hasMigratedCompletions2;
    @Accessors(fluent = true)
    @SerializedName("claimed_talisman")
    private boolean hasClaimedTalisman;
    @SerializedName("bop_bonus")
    private String bopBonus;
    @SerializedName("selected_symbol")
    private String selectedSymbol;
    @SerializedName("emblem_unlocks")
    private @NotNull ConcurrentList<String> emblemUnlocks = Concurrent.newList();
    @SerializedName("fishing_festival_sharks_killed")
    private int fishingFestivalSharksKilled;

    public int getLevel() {
        return (int) Math.floor(this.getExperience() / 100.0);
    }

}
