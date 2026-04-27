package api.simplified.hypixel.profile_stats;

import api.simplified.hypixel.profile_stats.data.Data;
import api.simplified.hypixel.profile_stats.data.ItemData;
import api.simplified.hypixel.profile_stats.data.ObjectData;
import api.simplified.hypixel.profile_stats.data.PlayerDataHelper;
import api.simplified.hypixel.profile_stats.data.StatData;
import api.simplified.hypixel.response.skyblock.SkyBlockIsland;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import api.simplified.hypixel.response.skyblock.island.Banking;
import api.simplified.hypixel.response.skyblock.member.AccessoryBag;
import api.simplified.hypixel.response.skyblock.member.pet.OwnedPet;
import dev.sbs.skyblockdata.SkyBlockData;
import dev.sbs.skyblockdata.common.Rarity;
import dev.sbs.skyblockdata.model.*;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.atomic.AtomicMap;
import dev.simplified.collection.linked.ConcurrentLinkedMap;
import dev.simplified.collection.tuple.pair.Pair;
import lib.minecraft.nbt.tags.collection.CompoundTag;
import lib.minecraft.nbt.tags.primitive.StringTag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Getter
@SuppressWarnings("unused")
public class ProfileStats extends StatData<ProfileStats.Type> {

    private final double damageMultiplier;
    private final AccessoryBag accessoryBag;
    private final Optional<OwnedPet> activePet;
    private final ConcurrentList<Optional<ItemData>> armor = Concurrent.newList();
    private final ConcurrentList<BonusPetAbilityStat> bonusPetAbilityStatModels = Concurrent.newList();
    private Optional<BonusArmorSet> bonusArmorSetModel = Optional.empty();
    private boolean bonusCalculated;
    @Getter(AccessLevel.NONE)
    private final ConcurrentMap<String, Double> expressionVariables = Concurrent.newMap();

    public ProfileStats(@NotNull SkyBlockIsland skyBlockIsland, @NotNull SkyBlockMember member) {
        this(skyBlockIsland, member, true);
    }

    public ProfileStats(@NotNull SkyBlockIsland skyBlockIsland, @NotNull SkyBlockMember member, boolean calculateBonusStats) {
        // --- Initialize ---
        ConcurrentList<Stat> statModels = SkyBlockData.getRepository(Stat.class).findAll();
        Arrays.stream(Type.values()).forEach(type -> {
            this.stats.put(type, Concurrent.newLinkedMap());
            statModels.forEach(statModel -> this.stats.get(type).put(statModel, new Data()));
        });
        statModels.forEach(statModel -> this.addBase(this.stats.get(Type.BASE_STATS).get(statModel), statModel.getBase()));
        this.activePet = member.getPets().getActivePet();
        this.accessoryBag = member.getAccessoryBag();

        // --- Populate Default Expression Variables ---
        this.getActivePet().ifPresent(activePet -> this.expressionVariables.put("PET_LEVEL", (double) activePet.getLevel()));
        this.expressionVariables.put("SKILL_AVERAGE", member.getSkills().getAverage());
        this.expressionVariables.put("SKYBLOCK_LEVEL", (double) member.getLeveling().getLevel());
        this.expressionVariables.put("BESTIARY_MILESTONE", (double) member.getBestiary().getMilestone());
        this.expressionVariables.put("BANK", skyBlockIsland.getBanking().map(Banking::getBalance).orElse(0.0));
        SkyBlockData.getRepository(Skill.class)
            .findAll()
            .forEach(skillModel -> this.expressionVariables.put(
                String.format("SKILL_LEVEL_%s", skillModel.getId()),
                (double) member.getSkills().getSkill(skillModel.getId()).getLevel()
            ));

        // TODO(profile_stats-restore): Dungeons.getDungeon(type).asEnhanced() + getClass(type).asEnhanced() no longer exist.
        // Re-wire once the new Dungeons API exposes level lookups, or restore .asEnhanced() helpers on DungeonData/DungeonClass.
        /*
        for (FloorData.Type dungeonType : FloorData.Type.values()) {
            if (dungeonType == FloorData.Type.UNKNOWN) continue;
            this.expressionVariables.put(
                String.format("DUNGEON_LEVEL_%s", dungeonType.name()),
                (double) member.getDungeons()
                    .getDungeon(dungeonType)
                    .asEnhanced()
                    .getLevel()
            );
        }

        for (FloorData.Class.Type classType : FloorData.Class.Type.values()) {
            if (classType == FloorData.Class.Type.UNKNOWN) continue;
            this.expressionVariables.put(
                String.format("DUNGEON_CLASS_LEVEL_%s", classType.name()),
                (double) member.getDungeons()
                    .getClass(classType)
                    .asEnhanced()
                    .getLevel()
            );
        }
        */

        // TODO(profile_stats-restore): SkyBlockMember.getCollection(CollectionModel) removed; the new
        // member.getCollection() is a zero-arg accessor. Re-wire once a per-CollectionModel state
        // lookup is re-exposed.
        /*
        SkyBlockData.getRepository(Collection.class)
            .stream()
            .map(member::getCollection)
            .flatMap(collection -> collection.getCollected().stream())
            .forEach(collectionItemEntry -> this.expressionVariables.put(String.format("COLLECTION_%s", collectionItemEntry.getKey().getItem().getItemId()), (double) collectionItemEntry.getValue()));
        */

        // --- Load Damage Multiplier ---
        this.damageMultiplier = SkyBlockData.getRepository(Skill.class)
            .findFirst(Skill::getId, "COMBAT")
            .map(skillModel -> {
                int skillLevel = member.getSkills()
                    .getSkill(skillModel.getId())
                    .getLevel();

                if (skillLevel > 0) {
                    return skillModel.getLevels()
                        .stream()
                        .filter(skillLevelModel -> skillLevelModel.getLevel() <= skillLevel)
                        .map(Skill.Level::getEffects)
                        .flatMap(map -> map.entrySet().stream())
                        .mapToDouble(Map.Entry::getValue)
                        .sum();
                }

                return 0.0;
            })
            .orElse(0.0) / 100.0;

        // --- Load Player Stats ---
        this.loadSkills(member);
        this.loadSlayers(member);
        this.loadDungeons(member);
        this.loadArmor(member);
        this.loadAccessories();
        this.loadActivePet(member);
        this.loadActivePotions(member);
        this.loadPetScore(member);
        this.loadMiningCore(member);
        this.loadCenturyCakes(member);
        this.loadEssencePerks(member);
        this.loadLevels(member);
        this.loadBestiary(member);
        this.loadBoosterCookie(member);
        this.loadMelodyHarp(member);
        this.loadJacobsPerks(member);

        if (calculateBonusStats) {
            ConcurrentMap<String, Double> expressionVariables = this.getExpressionVariables();
            // --- Load Bonus Accessory Item Stats ---
            this.getAccessoryBag().getAccessories().forEach(accessoryData -> accessoryData.calculateBonus(expressionVariables));

            // --- Load Bonus Armor Stats ---
            this.getArmor()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(itemData -> itemData.calculateBonus(expressionVariables));

            // --- Load Armor Multiplier Enchantments ---
            this.getArmor()
                .stream()
                .flatMap(Optional::stream)
                .forEach(itemData -> itemData.getEnchantments().forEach((enchantment, value) -> itemData.getEnchantmentStats().get(enchantment)
                    .stream()
                    .filter(sub -> sub.getStat().isPresent())
                    .filter(sub -> sub.getType() == Stat.Type.PERCENT || sub.getType() == Stat.Type.PLUS_PERCENT)
                    .forEach(sub -> {
                        double enchantMultiplier = 1 + sub.getValues().entrySet().stream()
                            .filter(e -> value >= e.getKey())
                            .mapToDouble(Map.Entry::getValue)
                            .sum() / 100.0;

                        this.stats.forEach((type, statEntries) -> {
                            Data statData = statEntries.get(sub.getStat().get());

                            // Apply Multiplier
                            this.setBase(statData, statData.getBase() * enchantMultiplier);
                            this.setBonus(statData, statData.getBonus() * enchantMultiplier);
                        });
                    }))
                );

            // --- Load Bonus Pet Item Stats ---
            ConcurrentMap<String, Double> petExpressionVariables = this.getExpressionVariables();
            this.getBonusPetAbilityStatModels()
                .stream()
                .filter(BonusPetAbilityStat::isPercentage)
                .filter(BonusPetAbilityStat::noRequiredItem)
                .filter(BonusPetAbilityStat::noRequiredMobType)
                .forEach(bonusPetAbilityStat -> {
                    // Handle Stats
                    this.getStats().forEach((type, statEntries) -> statEntries.forEach((statModel, statData) -> {
                        this.setBase(statData, PlayerDataHelper.handleBonusEffects(statModel, statData.getBase(), null, petExpressionVariables, bonusPetAbilityStat));
                        this.setBonus(statData, PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), null, petExpressionVariables, bonusPetAbilityStat));
                    }));

                    // Handle Armor
                    this.getArmor()
                        .stream()
                        .flatMap(Optional::stream)
                        .forEach(itemData -> itemData.getStats().forEach((type, statEntries) -> statEntries.forEach((statModel, statData) -> {
                            this.setBase(statData, PlayerDataHelper.handleBonusEffects(statModel, statData.getBase(), itemData.getCompoundTag(), petExpressionVariables, bonusPetAbilityStat));
                            this.setBonus(statData, PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), itemData.getCompoundTag(), petExpressionVariables, bonusPetAbilityStat));
                        })));

                    // Handle Accessories
                    this.getAccessoryBag().getAccessories().forEach(accessoryData -> accessoryData.getStats().forEach((type, statEntries) -> statEntries.forEach((statModel, statData) -> {
                        this.setBase(statData, PlayerDataHelper.handleBonusEffects(statModel, statData.getBase(), accessoryData.getCompoundTag(), petExpressionVariables, bonusPetAbilityStat));
                        this.setBonus(statData, PlayerDataHelper.handleBonusEffects(statModel, statData.getBonus(), accessoryData.getCompoundTag(), petExpressionVariables, bonusPetAbilityStat));
                    })));
                });

            // TODO: Load Post Bonus Stats
        }
    }

    public ConcurrentMap<String, Double> getExpressionVariables() {
        ConcurrentMap<String, Double> expressionVariables = Concurrent.newMap(this.expressionVariables);
        this.getAllStats().forEach((statModel, statData) -> expressionVariables.put(String.format("STAT_%s", statModel.getId()), statData.getTotal()));
        return expressionVariables;
    }

    public ConcurrentLinkedMap<Stat, Data> getCombinedStats() {
        return this.getCombinedStats(false);
    }

    public ConcurrentLinkedMap<Stat, Data> getCombinedStats(boolean optimizerConstant) {
        // Initialize
        ConcurrentLinkedMap<Stat, Data> totalStats = SkyBlockData.getRepository(Stat.class)
            .findAll()
            .stream()
            .map(statModel -> Pair.of(statModel, new Data()))
            .collect(Concurrent.toLinkedMap());

        // Collect Stat Data
        this.getStats()
            .stream()
            .filter(entry -> !optimizerConstant || entry.getKey().isOptimizerConstant())
            .forEach(entry -> entry.getValue().forEach((statModel, statData) -> {
                this.addBase(totalStats.get(statModel), statData.getBase());
                this.addBonus(totalStats.get(statModel), statData.getBonus());
            }));

        // Collect Accessory Data
        this.getAccessoryBag()
            .getAccessories()
            .stream()
            .map(StatData::getStats)
            .forEach(statTypeEntries -> statTypeEntries.stream()
                .filter(entry -> !optimizerConstant || entry.getKey().isOptimizerConstant())
                .forEach(entry -> entry.getValue().forEach((statModel, statData) -> {
                    this.addBase(totalStats.get(statModel), statData.getBase());
                    this.addBonus(totalStats.get(statModel), statData.getBonus());
                }))
            );

        // Collect Armor Data
        this.getArmor()
            .stream()
            .flatMap(Optional::stream)
            .map(StatData::getStats)
            .forEach(statTypeEntries -> statTypeEntries.stream()
                .filter(entry -> !optimizerConstant || entry.getKey().isOptimizerConstant())
                .forEach(entry -> entry.getValue().forEach((statModel, statData) -> {
                    this.addBase(totalStats.get(statModel), statData.getBase());
                    this.addBonus(totalStats.get(statModel), statData.getBonus());
                }))
            );

        return totalStats;
    }

    @Override
    protected Type[] getAllTypes() {
        return ProfileStats.Type.values();
    }

    private void loadAccessories() {
        // Accessory Power Stats
        this.getAccessoryBag()
            .getSelectedPowerStats()
            .forEach((key, value) -> SkyBlockData.getRepository(Stat.class)
                .findFirst(Stat::getId, key)
                .ifPresent(statModel -> this.addBonus(
                    this.stats.get(Type.ACCESSORY_POWER).get(statModel),
                    value
                ))
            );
    }

    private void loadActivePet(SkyBlockMember member) {
        if (this.getActivePet().isEmpty())
            return;

        OwnedPet activePet = this.getActivePet().get();

        if (activePet.getId().isEmpty())
            return;

        Optional<Pet> optionalPet = SkyBlockData.getRepository(Pet.class).findFirst(Pet::getId, activePet.getId());
        if (optionalPet.isEmpty())
            return;

        Pet petModel = optionalPet.get();
        Rarity petRarity = activePet.getRarity();

        // Load Rarity Filtered Pet Stats
        petModel.getStats(petRarity)
            .forEach(substitute -> substitute.getStat()
                .ifPresent(stat -> {
                    Pet.Substitute.Value value = substitute.getValues().get(petRarity);
                    if (value != null)
                        this.addBonus(this.stats.get(Type.ACTIVE_PET).get(stat), value.getBase() + (value.getScalar() * activePet.getLevel()));
                })
            );

        // Save Pet Stats to Expression Variables
        this.stats.get(Type.ACTIVE_PET).forEach((statModel, statData) -> this.expressionVariables.put(String.format("STAT_PET_%s", statModel.getId()), statData.getTotal()));

        // Load Rarity Filtered Ability Stats
        petModel.getAbilities(petRarity).forEach(ability -> {
            // Load Bonus Pet Ability Stats
            SkyBlockData.getRepository(BonusPetAbilityStat.class)
                .findFirst(
                    Pair.of(BonusPetAbilityStat::getPetId, petModel.getId()),
                    Pair.of(BonusPetAbilityStat::getAbilityName, ability.getName())
                )
                .ifPresent(this.bonusPetAbilityStatModels::add);

            ability.getStats(petRarity).forEach(substitute -> {
                Pet.Substitute.Value value = substitute.getValues().get(petRarity);
                double abilityValue = (value != null)
                    ? value.getBase() + (value.getScalar() * activePet.getLevel())
                    : 0.0;

                // Save Ability Stat
                substitute.getStat().ifPresent(stat ->
                    this.addBonus(this.stats.get(Type.ACTIVE_PET).get(stat), abilityValue)
                );

                // Store Bonus Pet Ability
                String statKey = substitute.getStat().map(stat -> "_" + stat.getId()).orElse("");
                this.expressionVariables.put(String.format("PET_ABILITY_%s%s", ability.getName(), statKey), abilityValue);
            });
        });

        // Handle Static Pet Item Bonuses
        String heldItemId = activePet.getHeldItem().orElse("");
        if (!heldItemId.isEmpty()) {
            SkyBlockData.getRepository(PetItem.class)
                .findFirst(PetItem::getId, heldItemId)
                .filter(PetItem::notPercentage)
                .ifPresent(petItem -> petItem.getStats().forEach(sub ->
                    sub.getStat().ifPresent(stat -> {
                        double statValue = sub.getValues().getOrDefault(1, 0.0);
                        this.addBonus(this.stats.get(Type.ACTIVE_PET).get(stat), statValue);
                    })
                ));
        }

        // Handle Static Pet Stat Bonuses
        ConcurrentMap<String, Double> petExpressionVariables = this.getExpressionVariables();
        this.getBonusPetAbilityStatModels()
            .stream()
            .filter(BonusPetAbilityStat::notPercentage)
            .filter(BonusPetAbilityStat::noRequiredItem)
            .filter(BonusPetAbilityStat::noRequiredMobType)
            .forEach(bonusPetAbilityStat -> this.stats.get(Type.ACTIVE_PET)
                .forEach((statModel, statData) -> this.setBonus(
                    statData,
                    PlayerDataHelper.handleBonusEffects(
                        statModel,
                        statData.getBonus(),
                        null,
                        petExpressionVariables,
                        bonusPetAbilityStat
                    ))
                )
            );

        // Handle Percentage Pet Item Bonuses
        if (!heldItemId.isEmpty()) {
            SkyBlockData.getRepository(PetItem.class)
                .findFirst(PetItem::getId, heldItemId)
                .filter(PetItem::isPercentage)
                .ifPresent(petItem -> petItem.getStats().forEach(sub ->
                    sub.getStat().ifPresent(stat -> {
                        double statMultiplier = 1 + (sub.getValues().getOrDefault(1, 0.0) / 100.0);
                        Data statData = this.stats.get(Type.ACTIVE_PET).get(stat);
                        this.setBonus(statData, statData.getBonus() * statMultiplier);
                    })
                ));
        }
    }

    private void loadActivePotions(SkyBlockMember member) {
        member.getPlayerData()
            .getActivePotions()
            .stream()
            .filter(potion -> !member.getPlayerData().getDisabledPotions().contains(potion.getEffect()))
            .forEach(potion -> {
                ConcurrentMap<Stat, Double> potionStatEffects = Concurrent.newMap();

                // Load Potion
                SkyBlockData.getRepository(Potion.class)
                    .findFirst(Potion::getId, potion.getEffect().toUpperCase())
                    .ifPresent(potionModel -> potionModel.getStats()
                        .stream()
                        .filter(sub -> sub.getValues().containsKey(potion.getLevel()))
                        .forEach(sub -> sub.getStat().ifPresent(stat ->
                            potionStatEffects.put(stat, sub.getValues().get(potion.getLevel()) + potionStatEffects.getOrDefault(stat, 0.0))
                        ))
                    );

                // Brew modifiers skipped for now (brew model migration pending)

                // Save Active Potions
                potionStatEffects.forEach((statModel, value) -> this.addBonus(this.stats.get(Type.ACTIVE_POTIONS).get(statModel), value));
            });
    }

    private void loadArmor(SkyBlockMember member) {
        if (member.getInventory().getArmor() != null) {
            ConcurrentList<Item> items = SkyBlockData.getRepository(Item.class).findAll();
            ConcurrentList<Pair<CompoundTag, Optional<Item>>> armorItemModels = member.getInventory().getArmor()
                .getNbtData()
                .<CompoundTag>getListTag("i")
                .stream()
                .map(itemTag -> Pair.of(
                    itemTag,
                    items.findFirst(Item::getId, itemTag.getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY).getValue())
                ))
                .collect(Concurrent.toList())
                .reversed();

            this.bonusArmorSetModel = SkyBlockData.getRepository(BonusArmorSet.class).findFirst(
                Pair.of(BonusArmorSet::getHelmetItem, armorItemModels.get(0).right().orElse(null)),
                Pair.of(BonusArmorSet::getChestplateItem, armorItemModels.get(1).right().orElse(null)),
                Pair.of(BonusArmorSet::getLeggingsItem, armorItemModels.get(2).right().orElse(null)),
                Pair.of(BonusArmorSet::getBootsItem, armorItemModels.get(3).right().orElse(null))
            );

            armorItemModels.forEach(armorItemModelPair -> {
                ItemData itemData = null;

                if (armorItemModelPair.left().notEmpty() && armorItemModelPair.right().isPresent())
                    itemData = new ItemData(
                        armorItemModelPair.right().get(),
                        armorItemModelPair.left()
                    );

                this.armor.add(Optional.ofNullable(itemData));
            });
        }
    }

    private void loadBestiary(SkyBlockMember member) {
        // TODO(profile_stats-restore): Bestiary.asEnhanced() no longer exists. Bestiary.getMilestone()
        // still returns an int directly, so once the EnhancedBestiary surface is re-established
        // (or we accept the raw value), re-enable this block.
        /*
        SkyBlockData.getRepository(Stat.class)
            .findFirst(Stat::getId, "HEALTH")
            .ifPresent(healthStatModel -> this.addBase(this.stats.get(Type.BESTIARY).get(healthStatModel), member.getBestiary().asEnhanced().getMilestone() * 2.0));
        */
    }

    private void loadBoosterCookie(SkyBlockMember member) {
        if (!member.isBoosterCookieActive())
            return;

        SkyBlockData.getRepository(Stat.class)
            .findFirst(Stat::getId, "MAGIC_FIND")
            .ifPresent(magicFindStatModel -> this.addBase(this.stats.get(Type.BOOSTER_COOKIE).get(magicFindStatModel), 15));

        SkyBlockData.getRepository(Stat.class)
            .matchAll(statModel -> statModel.getId().endsWith("_WISDOM"))
            .forEach(wisdomStateModel -> this.addBase(this.stats.get(Type.BOOSTER_COOKIE).get(wisdomStateModel), 25));
    }

    private void loadCenturyCakes(SkyBlockMember member) {
        member.getPlayerData()
            .getCenturyCakes()
            .stream()
            .filter(centuryCake -> centuryCake.getExpiresAt().getRealTime() > System.currentTimeMillis())
            .forEach(centuryCake -> this.addBonus(this.stats.get(Type.CENTURY_CAKES).get(centuryCake.getStat()), centuryCake.getAmount()));
    }

    private void loadDungeons(SkyBlockMember member) {
        // TODO(profile_stats-restore): DungeonData.asEnhanced() removed during the Hypixel-API extraction.
        // Re-enable once Dungeons exposes a stable "dungeon level" accessor.
        /*
        for (FloorData.Type dungeonType : FloorData.Type.values()) {
            if (dungeonType == FloorData.Type.UNKNOWN) continue;

            int dungeonLevel = member.getDungeons()
                .getDungeon(dungeonType)
                .asEnhanced()
                .getLevel();

            if (dungeonLevel > 0) {
                SkyBlockData.getRepository(Stat.class)
                    .findFirst(Stat::getId, "HEALTH")
                    .ifPresent(healthStat -> this.addBase(this.stats.get(Type.DUNGEONS).get(healthStat), dungeonLevel * 2.0));
            }
        }
        */
    }

    private void loadEssencePerks(SkyBlockMember member) {
        member.getPlayerData()
            .getShopPerks()
            .forEach(entry -> SkyBlockData.getRepository(ShopPerk.class)
                .findFirst(ShopPerk::getId, entry.getKey().toUpperCase())
                .ifPresent(shopPerk -> shopPerk.getStats()
                    .forEach(sub -> sub.getStat()
                        .ifPresent(stat -> {
                            double levelValue = sub.getValues().getOrDefault(entry.getValue(), 0.0);
                            this.addBonus(this.stats.get(Type.ESSENCE).get(stat), levelValue);
                        })
                    )
                )
            );
    }

    private void loadJacobsPerks(SkyBlockMember member) {
        SkyBlockData.getRepository(Stat.class).findFirst(Stat::getId, "FARMING_FORTUNE")
            .ifPresent(farmingFortuneStatModel -> this.addBase(this.stats.get(Type.JACOBS_FARMING).get(farmingFortuneStatModel), member.getJacobsContest().getDoubleDrops() * 4.0));
    }

    private void loadLevels(SkyBlockMember member) {
        SkyBlockData.getRepository(Stat.class).findFirst(Stat::getId, "HEALTH")
            .ifPresent(healthStatModel -> this.addBase(this.stats.get(Type.SKYBLOCK_LEVELS).get(healthStatModel), member.getLeveling().getLevel() * 5.0));
        SkyBlockData.getRepository(Stat.class).findFirst(Stat::getId, "STRENGTH")
            .ifPresent(strengthStatModel -> this.addBase(this.stats.get(Type.SKYBLOCK_LEVELS).get(strengthStatModel), member.getLeveling().getLevel() / 5.0));
    }

    private void loadMelodyHarp(SkyBlockMember member) {
        member.getForaging()
            .getMelodyHarp()
            .getSongs()
            .forEach((songName, songData) -> SkyBlockData.getRepository(MelodySong.class)
                .findFirst(MelodySong::getId, songName.toUpperCase())
                .ifPresent(melodySongModel -> SkyBlockData.getRepository(Stat.class).findFirst(Stat::getId, "INTELLIGENCE")
                    .ifPresent(statModel -> this.addBonus(this.stats.get(Type.MELODYS_HARP).get(statModel), melodySongModel.getIntelligenceReward()))
                )
            );
    }

    private void loadMiningCore(SkyBlockMember member) {
        // TODO(profile_stats-restore): HeartOfTheMountain.getNodes() removed.
        // Re-enable once the new mining API exposes node/level iteration.
        /*
        member.getMining()
            .getNodes()
            .forEach((key, level) -> SkyBlockData.getRepository(HotmPerk.class)
                .findFirst(HotmPerk::getId, key.toUpperCase())
                .ifPresent(hotmPerk -> hotmPerk.getStats()
                    .forEach(sub -> sub.getStat()
                        .ifPresent(stat -> {
                            double statValue = sub.getValues().getOrDefault(level, 0.0);
                            this.addBonus(this.stats.get(Type.MINING_CORE).get(stat), statValue);
                        })
                    )
                )
            );
        */
    }

    private void loadPetScore(SkyBlockMember member) {
        SkyBlockData.getRepository(Stat.class)
            .findFirst(Stat::getId, "MAGIC_FIND")
            .ifPresent(magicFindStatModel -> this.addBase(this.stats.get(Type.PET_SCORE).get(magicFindStatModel), Pet.PET_SCORE
                .stream()
                .filter(breakpoint -> member.getPets().getPetScore() >= breakpoint)
                .collect(Concurrent.toList())
                .size())
            );
    }

    private void loadSkills(SkyBlockMember member) {
        SkyBlockData.getRepository(Skill.class)
            .stream()
            .forEach(skillModel -> {
                int skillLevel = member.getSkills()
                    .getSkill(skillModel.getId())
                    .getLevel();

                if (skillLevel > 0) {
                    skillModel.getLevels()
                        .stream()
                        .filter(skillLevelModel -> skillLevelModel.getLevel() <= skillLevel)
                        .map(Skill.Level::getEffects)
                        .flatMap(AtomicMap::stream)
                        .forEach(entry -> SkyBlockData.getRepository(Stat.class)
                            .findFirst(Stat::getId, entry.getKey())
                            .ifPresent(statModel -> this.addBase(this.stats.get(Type.SKILLS).get(statModel), entry.getValue()))
                        );
                }
            });
    }

    private void loadSlayers(SkyBlockMember member) {
        // TODO(profile_stats-restore): Slayers.getSlayer(String) removed. Re-enable once
        // Slayers exposes per-boss lookup (by id or SlayerBoss reference).
        /*
        SkyBlockData.getRepository(Slayer.class)
            .stream()
            .forEach(slayerModel -> {
                int slayerLevel = member.getSlayers()
                    .getSlayer(slayerModel.getId())
                    .getLevel();

                if (slayerLevel > 0) {
                    slayerModel.getLevels()
                        .stream()
                        .filter(slayerLevelModel -> slayerLevelModel.getLevel() <= slayerLevel)
                        .map(Slayer.Level::getEffects)
                        .flatMap(map -> map.entrySet().stream())
                        .forEach(entry -> SkyBlockData.getRepository(Stat.class)
                            .findFirst(Stat::getId, entry.getKey())
                            .ifPresent(statModel -> this.addBase(this.stats.get(Type.SKILLS).get(statModel), entry.getValue()))
                        );
                }
            });
        */
    }

    @Getter
    @RequiredArgsConstructor
    public enum Type implements ObjectData.Type {

        ACCESSORY_POWER(false),
        ACTIVE_PET(true),
        ACTIVE_POTIONS(true),
        BASE_STATS(true),
        BESTIARY(true),
        BOOSTER_COOKIE(true),
        CENTURY_CAKES(true),
        DUNGEONS(true),
        ESSENCE(true),
        SKYBLOCK_LEVELS(true),
        JACOBS_FARMING(true),
        MELODYS_HARP(true),
        MINING_CORE(true),
        PET_SCORE(true),
        SKILLS(true),
        SLAYERS(true);

        private final boolean optimizerConstant;

    }

}
