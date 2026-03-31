package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.Extract;
import dev.sbs.api.io.gson.Lenient;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class ChocolateFactory {

    // Chocolate
    private long chocolate;
    @SerializedName("total_chocolate")
    private long totalChocolate;
    @SerializedName("chocolate_since_prestige")
    private long chocolateSincePrestige;
    private int chocolateLevel;
    @SerializedName("last_viewed_chocolate_factory")
    private Instant lastViewed;

    // Collection
    @SerializedName("rabbit_sort")
    private @NotNull RabbitSort rabbitSort = RabbitSort.A_TO_Z;
    @SerializedName("rabbit_filter")
    private @NotNull RabbitFilter rabbitFilter = RabbitFilter.NONE;
    @SerializedName("rabbit_hotspit_filer")
    private @NotNull String rabbitHotspot = "NONE";

    // Rabbits
    private @NotNull ConcurrentMap<RabbitEmployee, Integer> employees = Concurrent.newMap();
    @SerializedName("rabbit_barn_capacity_level")
    private int barnCapacity;
    @SerializedName("el_dorado_progress")
    private int elDoradoProgress;
    @Lenient
    private @NotNull ConcurrentMap<String, Integer> rabbits = Concurrent.newMap();
    @Extract("rabbitMap.collected_eggs")
    private @NotNull ConcurrentMap<String, Long> eggs = Concurrent.newMap();
    @Extract("rabbitMap.collected_locations")
    private @NotNull ConcurrentMap<String, ConcurrentList<String>> locations = Concurrent.newMap();

    // Golden Rabbits
    @SerializedName("golden_click_amount")
    private int goldenClickAmount;
    @SerializedName("golden_click_year")
    private int goldenClickYear;

    // Upgrades
    @SerializedName("click_upgrades")
    private int clickUpgrades;
    @SerializedName("time_tower")
    private @NotNull ChocolateTimeTower timeTower = new ChocolateTimeTower();
    @SerializedName("rabbit_rarity_upgrades")
    private int rabbitShrine;
    @SerializedName("chocolate_multiplier_upgrades")
    private int coachJackrabbit;
    @SerializedName("rabbit_hitmen")
    private @NotNull RabbitHitman hitman = new RabbitHitman();

    // Shop
    private @NotNull ChocolateShop shop = new ChocolateShop();
    @SerializedName("supreme_chocolate_bars")
    private int remainingSupremeChocolateBars;
    @SerializedName("refined_dark_cacao_truffles")
    private int remainingDarkCacaoTruffles;

}
