package dev.sbs.minecraftapi.client.hypixel.response.skyblock.resource;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.data.json.JsonModel;
import dev.sbs.minecraftapi.client.mojang.profile.MojangProperty;
import dev.sbs.minecraftapi.skyblock.Rarity;
import dev.sbs.minecraftapi.skyblock.resource.Item;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Optional;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonItem implements Item, JsonModel {

    // Expected Data
    private @NotNull String material = "";
    private @NotNull String id = "";
    @SerializedName("name")
    private @NotNull String displayName = "";
    @SerializedName("tier")
    private @NotNull Rarity rarity = Rarity.COMMON;
    @SerializedName("soulbound")
    private @NotNull Soulbound soulboundStatus = Soulbound.NONE;

    // Possible Data
    private @NotNull Optional<Integer> durability = Optional.empty();
    private @NotNull Optional<String> category = Optional.empty();
    private @NotNull Optional<String> description = Optional.empty();
    private @NotNull Optional<Color> color = Optional.empty();
    private @NotNull Optional<String> origin = Optional.empty();
    private @NotNull Optional<MojangProperty> skin = Optional.empty();
    private @NotNull Optional<String> furniture = Optional.empty();
    private @NotNull Optional<String> crystal = Optional.empty();
    @SerializedName("museum_data")
    private @NotNull Optional<MuseumData> museumData = Optional.empty();
    @SerializedName("sword_type")
    private @NotNull Optional<String> swordType = Optional.empty();
    @SerializedName("private_island")
    private @NotNull Optional<String> miniIslandGenerator = Optional.empty();
    @SerializedName("npc_sell_price")
    private double npcSellPrice;
    @SerializedName("ability_damage_scaling")
    private double abilityDamageScaling;

    // Booleans
    private boolean glowing;
    private boolean unstackable;
    @SerializedName("museum")
    private boolean museumable;
    @SerializedName("can_have_attributes")
    private boolean attributable;
    @SerializedName("salvageable_from_recipe")
    private boolean salvageableFromRecipe;
    @SerializedName("cannot_reforge")
    private boolean notReforgeable;

    // Dungeons
    @SerializedName("gear_score")
    private int gearScore;
    @SerializedName("dungeon_item")
    private boolean dungeonItem;
    @SerializedName("dungeon_item_conversion_cost")
    private @NotNull ConcurrentMap<String, Object> dungeonizationCost = Concurrent.newMap();
    @SerializedName("catacombs_requirements")
    private @NotNull ConcurrentList<ConcurrentMap<String, Object>> catacombsRequirements = Concurrent.newList();

    // Rift
    @SerializedName("rift_transferrable")
    private boolean riftTransferable;
    @SerializedName("lose_motes_value_on_transfer")
    private boolean motesValueLostOnTransfer;
    @SerializedName("motes_sell_price")
    private double motesSellPrice;

    // Minions
    private @NotNull Optional<String> generator = Optional.empty();
    @SerializedName("generator_tier")
    private int generatorTier;

    // Other
    private @NotNull ConcurrentMap<String, Double> enchantments = Concurrent.newMap();
    @SerializedName("gemstone_slots")
    private @NotNull ConcurrentList<ConcurrentMap<String, Object>> gemstoneSlots = Concurrent.newList();
    @SerializedName("item_specific")
    private @NotNull ConcurrentMap<String, Object> itemSpecific = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Object> prestige = Concurrent.newMap();
    private @NotNull ConcurrentList<ConcurrentMap<String, Object>> requirements = Concurrent.newList();
    private @NotNull ConcurrentList<ConcurrentMap<String, Object>> salvages = Concurrent.newList();
    private @NotNull ConcurrentMap<String, Double> stats = Concurrent.newMap();
    @SerializedName("tiered_stats")
    private @NotNull ConcurrentMap<String, List<Double>> tieredStats = Concurrent.newMap();
    @SerializedName("upgrade_costs")
    private @NotNull ConcurrentList<ConcurrentList<ConcurrentMap<String, Object>>> upgradeCosts = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonItem jsonItem = (JsonItem) o;

        return new EqualsBuilder()
            .append(this.getNpcSellPrice(), jsonItem.getNpcSellPrice())
            .append(this.getAbilityDamageScaling(), jsonItem.getAbilityDamageScaling())
            .append(this.isGlowing(), jsonItem.isGlowing())
            .append(this.isUnstackable(), jsonItem.isUnstackable())
            .append(this.isMuseumable(), jsonItem.isMuseumable())
            .append(this.isAttributable(), jsonItem.isAttributable())
            .append(this.isSalvageableFromRecipe(), jsonItem.isSalvageableFromRecipe())
            .append(this.isNotReforgeable(), jsonItem.isNotReforgeable())
            .append(this.getGearScore(), jsonItem.getGearScore())
            .append(this.isDungeonItem(), jsonItem.isDungeonItem())
            .append(this.isRiftTransferable(), jsonItem.isRiftTransferable())
            .append(this.isMotesValueLostOnTransfer(), jsonItem.isMotesValueLostOnTransfer())
            .append(this.getMotesSellPrice(), jsonItem.getMotesSellPrice())
            .append(this.getGeneratorTier(), jsonItem.getGeneratorTier())
            .append(this.getMaterial(), jsonItem.getMaterial())
            .append(this.getId(), jsonItem.getId())
            .append(this.getDisplayName(), jsonItem.getDisplayName())
            .append(this.getRarity(), jsonItem.getRarity())
            .append(this.getSoulboundStatus(), jsonItem.getSoulboundStatus())
            .append(this.getDurability(), jsonItem.getDurability())
            .append(this.getCategory(), jsonItem.getCategory())
            .append(this.getDescription(), jsonItem.getDescription())
            .append(this.getColor(), jsonItem.getColor())
            .append(this.getOrigin(), jsonItem.getOrigin())
            .append(this.getSkin(), jsonItem.getSkin())
            .append(this.getFurniture(), jsonItem.getFurniture())
            .append(this.getCrystal(), jsonItem.getCrystal())
            .append(this.getMuseumData(), jsonItem.getMuseumData())
            .append(this.getSwordType(), jsonItem.getSwordType())
            .append(this.getMiniIslandGenerator(), jsonItem.getMiniIslandGenerator())
            .append(this.getDungeonizationCost(), jsonItem.getDungeonizationCost())
            .append(this.getCatacombsRequirements(), jsonItem.getCatacombsRequirements())
            .append(this.getGenerator(), jsonItem.getGenerator())
            .append(this.getEnchantments(), jsonItem.getEnchantments())
            .append(this.getGemstoneSlots(), jsonItem.getGemstoneSlots())
            .append(this.getItemSpecific(), jsonItem.getItemSpecific())
            .append(this.getPrestige(), jsonItem.getPrestige())
            .append(this.getRequirements(), jsonItem.getRequirements())
            .append(this.getSalvages(), jsonItem.getSalvages())
            .append(this.getStats(), jsonItem.getStats())
            .append(this.getTieredStats(), jsonItem.getTieredStats())
            .append(this.getUpgradeCosts(), jsonItem.getUpgradeCosts())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getMaterial())
            .append(this.getId())
            .append(this.getDisplayName())
            .append(this.getRarity())
            .append(this.getSoulboundStatus())
            .append(this.getDurability())
            .append(this.getCategory())
            .append(this.getDescription())
            .append(this.getColor())
            .append(this.getOrigin())
            .append(this.getSkin())
            .append(this.getFurniture())
            .append(this.getCrystal())
            .append(this.getMuseumData())
            .append(this.getSwordType())
            .append(this.getMiniIslandGenerator())
            .append(this.getNpcSellPrice())
            .append(this.getAbilityDamageScaling())
            .append(this.isGlowing())
            .append(this.isUnstackable())
            .append(this.isMuseumable())
            .append(this.isAttributable())
            .append(this.isSalvageableFromRecipe())
            .append(this.isNotReforgeable())
            .append(this.getGearScore())
            .append(this.isDungeonItem())
            .append(this.getDungeonizationCost())
            .append(this.getCatacombsRequirements())
            .append(this.isRiftTransferable())
            .append(this.isMotesValueLostOnTransfer())
            .append(this.getMotesSellPrice())
            .append(this.getGenerator())
            .append(this.getGeneratorTier())
            .append(this.getEnchantments())
            .append(this.getGemstoneSlots())
            .append(this.getItemSpecific())
            .append(this.getPrestige())
            .append(this.getRequirements())
            .append(this.getSalvages())
            .append(this.getStats())
            .append(this.getTieredStats())
            .append(this.getUpgradeCosts())
            .build();
    }

}
