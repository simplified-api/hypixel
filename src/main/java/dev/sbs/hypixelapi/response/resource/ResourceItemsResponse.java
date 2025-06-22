package dev.sbs.minecraftapi.client.hypixel.response.resource;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceItemsResponse {

    private boolean success;
    private long lastUpdated;
    private @NotNull ConcurrentList<Item> items = Concurrent.newList();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Item {

        private String id;
        private String name;
        private String material;
        private int durability;
        private String description;
        @SerializedName("tier")
        private String rarity;
        @SerializedName("category")
        private String itemType;
        private String color;

        private boolean glowing;
        private boolean unstackable;
        private boolean dungeonItem;
        private boolean museum;
        @SerializedName("can_have_attributes")
        private boolean attributable;
        @SerializedName("hidden_from_viewrecipe_command")
        private boolean hiddenFromViewrecipe;
        @SerializedName("salvageable_from_recipe")
        private boolean salvageableFromRecipe;
        @SerializedName("cannot_reforge")
        private boolean notReforgeable;
        @SerializedName("rift_transferrable")
        private boolean riftTransferrable;
        @SerializedName("lose_motes_value_on_transfer")
        private boolean riftLoseMotesValueOnTransfer;


        @SerializedName("motes_sell_price")
        private double riftMotesSellPrice;
        @SerializedName("npc_sell_price")
        private double npcSellPrice;
        @SerializedName("gear_score")
        private int gearScore;
        private String generator;
        @SerializedName("generator_tier")
        private int generatorTier;
        @SerializedName("ability_damage_scaling")
        private double abilityDamageScaling;
        private String origin;
        private String soulbound;
        private String furniture;
        @SerializedName("sword_type")
        private String swordType;
        private String skin;
        private String crystal;
        @SerializedName("private_island")
        private String privateIsland;

        private ConcurrentMap<String, Double> stats = Concurrent.newMap();
        @SerializedName("tiered_stats")
        private ConcurrentMap<String, List<Double>> tieredStats = Concurrent.newMap();
        private ConcurrentList<ConcurrentMap<String, Object>> requirements = Concurrent.newList();
        @SerializedName("catacombs_requirements")
        private ConcurrentList<ConcurrentMap<String, Object>> catacombsRequirements;
        //@Getter private ConcurrentList<ItemCatacombsRequirements> catacombsRequirements;
        @SerializedName("upgrade_costs")
        private ConcurrentList<ConcurrentList<ConcurrentMap<String, Object>>> upgradeCosts = Concurrent.newList();
        @SerializedName("gemstone_slots")
        private ConcurrentList<ConcurrentMap<String, Object>> gemstoneSlots = Concurrent.newList();
        private ConcurrentMap<String, Double> enchantments = Concurrent.newMap();
        @SerializedName("dungeon_item_conversion_cost")
        private ConcurrentMap<String, Object> dungeonItemConversionCost = Concurrent.newMap();
        private ConcurrentMap<String, Object> prestige = Concurrent.newMap();
        @SerializedName("item_specific")
        private ConcurrentMap<String, Object> itemSpecific = Concurrent.newMap();
        private ConcurrentList<ConcurrentMap<String, Object>> salvages = Concurrent.newList();

    }


}
