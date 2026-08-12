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
import api.simplified.hypixel.response.skyblock.member.CenturyCake;
import api.simplified.hypixel.response.skyblock.member.SkillTree;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonClass;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonData;
import api.simplified.hypixel.response.skyblock.member.pet.OwnedPet;
import api.simplified.hypixel.response.skyblock.member.pet.Pets;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.common.Rarity;
import api.simplified.skyblock.model.*;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentLinkedMap;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.atomic.AtomicMap;
import dev.simplified.collection.tuple.pair.Pair;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.StringTag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Every stat one member has on one profile, totalled from all sixteen sources that feed them.
 * <p>
 * This is the top of the derived layer and by far the heaviest thing in it. Skills, slayers,
 * dungeons, armour, accessories, the active pet, potions, century cakes, essence perks and the rest
 * each contribute separately, and the whole lot is resolved against the reference repositories, so a
 * session is required and construction is not cheap.
 * <p>
 * Order matters here in a way it does not elsewhere. Many bonuses are expressions over stats the
 * player already has, so the flat sources are totalled first, published as expression variables, and
 * only then are the conditional bonuses evaluated against them. Passing {@code false} for the bonus
 * pass stops after the flat sources, which is what an optimiser wants when it is about to vary the
 * gear anyway.
 */
@Getter
@SuppressWarnings("unused")
public class ProfileStats extends StatData<ProfileStats.Type> {

    /**
     * Damage scaling earned from combat levels, as a fraction rather than a percentage.
     */
    private final double damageMultiplier;

    /**
     * The member's accessory bag, already initialized with its member-scoped values.
     */
    private final AccessoryBag accessoryBag;

    /**
     * The pet currently summoned, empty when none is.
     */
    private final Optional<OwnedPet> activePet;

    /**
     * The four armour pieces, each empty when that slot is unfilled.
     */
    private final ConcurrentList<Optional<ItemData>> armor = Concurrent.newList();

    /**
     * Conditional bonuses declared for the active pet's abilities, gathered as the pet is read.
     */
    private final ConcurrentList<BonusPetAbilityStat> bonusPetAbilityStatModels = Concurrent.newList();

    /**
     * Set bonus the worn armour qualifies for, empty when the pieces do not form a set.
     */
    private Optional<BonusArmorSet> bonusArmorSetModel = Optional.empty();

    /**
     * Whether the conditional bonuses have already been evaluated.
     */
    private boolean bonusCalculated;

    @Getter(AccessLevel.NONE)
    private final ConcurrentMap<String, Double> expressionVariables = Concurrent.newMap();

    /**
     * Constructs a new {@code ProfileStats} with the conditional bonuses evaluated.
     *
     * @param skyBlockIsland the profile the member belongs to, read for the shared bank balance
     * @param member the member to total
     */
    public ProfileStats(@NotNull SkyBlockIsland skyBlockIsland, @NotNull SkyBlockMember member) {
        this(skyBlockIsland, member, true);
    }

    /**
     * Constructs a new {@code ProfileStats}, optionally stopping before the conditional bonuses.
     *
     * @param skyBlockIsland the profile the member belongs to, read for the shared bank balance
     * @param member the member to total
     * @param calculateBonusStats whether to evaluate the bonuses that depend on the flat totals
     */
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

        for (DungeonData.Type dungeonType : DungeonData.Type.values()) {
            if (dungeonType == DungeonData.Type.UNKNOWN) continue;
            this.expressionVariables.put(
                String.format("DUNGEON_LEVEL_%s", dungeonType.name()),
                (double) member.getDungeons()
                    .getDungeon(dungeonType)
                    .getLevel()
            );
        }

        for (DungeonClass.Type classType : DungeonClass.Type.values()) {
            if (classType == DungeonClass.Type.UNKNOWN) continue;
            this.expressionVariables.put(
                String.format("DUNGEON_CLASS_LEVEL_%s", classType.name()),
                (double) member.getDungeons()
                    .getClass(classType)
                    .getLevel()
            );
        }

        member.getCollection()
            .forEach((itemId, collected) -> this.expressionVariables.put(
                String.format("COLLECTION_%s", itemId),
                (double) collected
            ));

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

    /**
     * The player state a bonus expression can refer to, with the current stat totals folded in.
     * <p>
     * A fresh copy is built on every call, since the totals move as sources are added and a bonus
     * has to see the values as they stand when it runs.
     */
    public ConcurrentMap<String, Double> getExpressionVariables() {
        ConcurrentMap<String, Double> expressionVariables = Concurrent.newMap(this.expressionVariables);
        this.getAllStats().forEach((statModel, statData) -> expressionVariables.put(String.format("STAT_%s", statModel.getId()), statData.getTotal()));
        return expressionVariables;
    }

    /**
     * Every stat the member has, with the profile's own sources, the armour and the accessories all
     * added together.
     */
    public ConcurrentLinkedMap<Stat, Data> getCombinedStats() {
        return this.getCombinedStats(false);
    }

    /**
     * Totals every source, optionally keeping only the parts an optimiser can treat as fixed.
     * <p>
     * Restricting to the fixed parts leaves out the reforges and the accessory power, which are the
     * two things an optimiser varies - so what remains is the constant an optimiser can compute once
     * and add to each candidate.
     *
     * @param optimizerConstant whether to keep only the sources that do not vary with the gear
     * @return a fresh table covering every known stat
     */
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
        SkyBlockData.getRepository(Stat.class)
            .findFirst(Stat::getId, "HEALTH")
            .ifPresent(healthStatModel -> this.addBase(this.stats.get(Type.BESTIARY).get(healthStatModel), member.getBestiary().getMilestone() * 2.0));
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
            .filter(CenturyCake::isActive)
            .forEach(centuryCake -> SkyBlockData.getRepository(Stat.class)
                .findFirst(Stat::getId, centuryCake.getStatId())
                .ifPresent(statModel -> this.addBonus(this.stats.get(Type.CENTURY_CAKES).get(statModel), centuryCake.getAmount()))
            );
    }

    private void loadDungeons(SkyBlockMember member) {
        for (DungeonData.Type dungeonType : DungeonData.Type.values()) {
            if (dungeonType == DungeonData.Type.UNKNOWN) continue;

            int dungeonLevel = member.getDungeons()
                .getDungeon(dungeonType)
                .getLevel();

            if (dungeonLevel > 0) {
                SkyBlockData.getRepository(Stat.class)
                    .findFirst(Stat::getId, "HEALTH")
                    .ifPresent(healthStat -> this.addBase(this.stats.get(Type.DUNGEONS).get(healthStat), dungeonLevel * 2.0));
            }
        }
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
        member.getSkillTree()
            .getNodes(SkillTree.Tree.MINING)
            .map(SkillTree.Skill::getEntries)
            .ifPresent(entries -> entries.stream()
                .filter(entry -> entry.getValue().isEnabled()) // a perk switched off grants nothing
                .forEach(entry -> SkyBlockData.getRepository(HotmPerk.class)
                    .findFirst(HotmPerk::getId, entry.getKey().toUpperCase())
                    .ifPresent(hotmPerk -> hotmPerk.getStats()
                        .forEach(sub -> sub.getStat()
                            .ifPresent(stat -> {
                                double statValue = sub.getValues().getOrDefault(entry.getValue().getLevel(), 0.0);
                                this.addBonus(this.stats.get(Type.MINING_CORE).get(stat), statValue);
                            })
                        )
                    )
                )
            );
    }

    private void loadPetScore(SkyBlockMember member) {
        SkyBlockData.getRepository(Stat.class)
            .findFirst(Stat::getId, "MAGIC_FIND")
            .ifPresent(magicFindStatModel -> this.addBase(this.stats.get(Type.PET_SCORE).get(magicFindStatModel), Pets.PET_SCORE
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
                        .flatMap(ConcurrentMap::stream)
                        .forEach(entry -> SkyBlockData.getRepository(Stat.class)
                            .findFirst(Stat::getId, entry.getKey())
                            .ifPresent(statModel -> this.addBase(this.stats.get(Type.SKILLS).get(statModel), entry.getValue()))
                        );
                }
            });
    }

    private void loadSlayers(SkyBlockMember member) {
        member.getSlayers()
            .getBosses()
            .forEach(slayerBoss -> SkyBlockData.getRepository(Slayer.class)
                .findFirst(Slayer::getId, slayerBoss.getId())
                .ifPresent(slayerModel -> {
                    int slayerLevel = slayerBoss.getLevel();

                    if (slayerLevel > 0) {
                        slayerModel.getLevels()
                            .stream()
                            .filter(slayerLevelModel -> slayerLevelModel.getLevel() <= slayerLevel)
                            .map(Slayer.Level::getEffects)
                            .flatMap(ConcurrentMap::stream)
                            .forEach(entry -> SkyBlockData.getRepository(Stat.class)
                                .findFirst(Stat::getId, entry.getKey())
                                .ifPresent(statModel -> this.addBase(this.stats.get(Type.SLAYERS).get(statModel), entry.getValue()))
                            );
                    }
                })
            );
    }

    /**
     * The sixteen sources a member's stats are split between.
     * <p>
     * All but the accessory power are fixed for the member, since the rest depend on progression
     * rather than on gear. The selected power is the one an optimiser is free to change, so it alone
     * has to be recomputed per candidate.
     */
    @Getter
    @RequiredArgsConstructor
    public enum Type implements ObjectData.Type {

        /**
         * Stats granted by the accessory bag's selected power, scaled by its magical power.
         */
        ACCESSORY_POWER(false),

        /**
         * Stats and ability stats from the pet currently summoned, scaled by its level.
         */
        ACTIVE_PET(true),

        /**
         * Stats from potion effects currently running.
         */
        ACTIVE_POTIONS(true),

        /**
         * The flat starting values every player has before anything is earned.
         */
        BASE_STATS(true),

        /**
         * Stats from bestiary milestones.
         */
        BESTIARY(true),

        /**
         * Stats from an active booster cookie.
         */
        BOOSTER_COOKIE(true),

        /**
         * Stats from century cakes still in date.
         */
        CENTURY_CAKES(true),

        /**
         * Stats from Catacombs levels and the selected dungeon class.
         */
        DUNGEONS(true),

        /**
         * Stats from essence perks bought in the corresponding menus.
         */
        ESSENCE(true),

        /**
         * Stats from SkyBlock levels.
         */
        SKYBLOCK_LEVELS(true),

        /**
         * Stats from Jacob's farming perks, the permanent farming fortune upgrades.
         */
        JACOBS_FARMING(true),

        /**
         * Stats from the songs completed on Melody's Harp.
         */
        MELODYS_HARP(true),

        /**
         * Stats from Heart of the Mountain perks.
         */
        MINING_CORE(true),

        /**
         * Stats from pet score, which counts the distinct pets collected by rarity.
         */
        PET_SCORE(true),

        /**
         * Stats from skill levels.
         */
        SKILLS(true),

        /**
         * Stats from slayer levels.
         */
        SLAYERS(true);

        /**
         * Whether this source is fixed for the member, so an optimiser need not recompute it.
         */
        private final boolean optimizerConstant;

    }

}
