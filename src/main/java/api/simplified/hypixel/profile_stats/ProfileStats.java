package api.simplified.hypixel.profile_stats;

import api.simplified.hypixel.profile_stats.data.Data;
import api.simplified.hypixel.profile_stats.data.ItemData;
import api.simplified.hypixel.profile_stats.data.PlayerDataHelper;
import api.simplified.hypixel.profile_stats.data.StatData;
import api.simplified.hypixel.profile_stats.data.StatHalf;
import api.simplified.hypixel.response.skyblock.SkyBlockIsland;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import api.simplified.hypixel.response.skyblock.island.Banking;
import api.simplified.hypixel.response.skyblock.member.AccessoryBag;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonClass;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonData;
import api.simplified.hypixel.response.skyblock.member.pet.OwnedPet;
import api.simplified.skyblock.model.BonusArmorSet;
import api.simplified.skyblock.model.BonusPetPerkStat;
import api.simplified.skyblock.model.Item;
import api.simplified.skyblock.model.Skill;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentLinkedMap;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.StringTag;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 * gear anyway. Within the flat pass order decides nothing, because a {@link StatSource} writes
 * through a sink it cannot read back.
 * <p>
 * {@link #compute(SkyBlockIsland, SkyBlockMember)} is the entry point. Totalling is something a
 * caller asks for by name, never something a decoded object does on its own - nothing on the wire
 * holds one of these and no accessor on a decoded object builds one.
 */
@Getter
@SuppressWarnings("unused")
public class ProfileStats extends StatData<StatSource> {

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
     * Set bonus the worn armour qualifies for, empty when the pieces do not form a set.
     */
    private Optional<BonusArmorSet> bonusArmorSetModel = Optional.empty();

    /**
     * Whether the conditional bonuses have already been evaluated.
     */
    private boolean bonusCalculated;

    @Getter(AccessLevel.NONE)
    private final StatContext context;

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
        this(ReferenceSnapshot.load(), skyBlockIsland, member, calculateBonusStats);
    }

    private ProfileStats(@NotNull ReferenceSnapshot reference, @NotNull SkyBlockIsland skyBlockIsland, @NotNull SkyBlockMember member, boolean calculateBonusStats) {
        super(reference);
        this.activePet = member.getPets().getActivePet();
        this.accessoryBag = member.getAccessoryBag();
        this.context = new StatContext(skyBlockIsland, member, this.accessoryBag, reference);

        // --- Resolve Gear ---
        // neither is a source - each is a list of tables of its own - and both resolve against the
        // shared snapshot here so nothing reaches for one of its own later
        this.accessoryBag.getAccessories(reference);
        this.loadArmor(reference, member);

        // --- Populate Default Expression Variables ---
        ConcurrentMap<String, Double> variables = this.context.getVariables();
        this.getActivePet().ifPresent(activePet -> variables.put("PET_LEVEL", (double) activePet.getLevel()));
        variables.put("SKILL_AVERAGE", member.getSkills().getAverage());
        variables.put("SKYBLOCK_LEVEL", (double) member.getLeveling().getLevel());
        variables.put("BESTIARY_MILESTONE", (double) member.getBestiary().getMilestone());
        variables.put("BANK", skyBlockIsland.getBanking().map(Banking::getBalance).orElse(0.0));
        reference.getSkills()
            .forEach(skillModel -> variables.put(
                String.format("SKILL_LEVEL_%s", skillModel.getId()),
                (double) member.getSkills().getSkill(skillModel.getId()).getLevel()
            ));

        for (DungeonData.Type dungeonType : DungeonData.Type.values()) {
            if (dungeonType == DungeonData.Type.UNKNOWN) continue;
            variables.put(
                String.format("DUNGEON_LEVEL_%s", dungeonType.name()),
                (double) member.getDungeons()
                    .getDungeon(dungeonType)
                    .getLevel()
            );
        }

        for (DungeonClass.Type classType : DungeonClass.Type.values()) {
            if (classType == DungeonClass.Type.UNKNOWN) continue;
            variables.put(
                String.format("DUNGEON_CLASS_LEVEL_%s", classType.name()),
                (double) member.getDungeons()
                    .getClass(classType)
                    .getLevel()
            );
        }

        member.getCollection()
            .forEach((itemId, collected) -> variables.put(
                String.format("COLLECTION_%s", itemId),
                (double) collected
            ));

        // --- Load Damage Multiplier ---
        this.damageMultiplier = reference.getSkills()
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

        // --- Flat Pass ---
        Arrays.stream(StatSource.values()).forEach(source -> source.contribute(this.context, this.table));
        this.context.publishTotals(this.table);

        if (calculateBonusStats) {
            // --- Load Bonus Accessory Item Stats ---
            this.getAccessoryBag().getAccessories().forEach(accessoryData -> accessoryData.calculateBonus(variables));

            // --- Load Bonus Armor Stats ---
            this.getArmor()
                .stream()
                .flatMap(Optional::stream)
                .forEach(itemData -> itemData.calculateBonus(variables));

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

                        // Apply Multiplier - an unwritten cell is zero, so rescaling it would write a zero
                        this.getStats().forEach((type, statEntries) -> Optional.ofNullable(statEntries.get(sub.getStat().get()))
                            .ifPresent(statData -> {
                                for (StatHalf half : StatHalf.values())
                                    half.set(statData, half.read(statData) * enchantMultiplier);
                            }));
                    }))
                );

            // --- Load Bonus Pet Item Stats ---
            this.context.publishTotals(this.table);
            this.getBonusPetPerkStatModels()
                .stream()
                .filter(BonusPetPerkStat::isPercentage)
                .filter(BonusPetPerkStat::noRequiredItem)
                .filter(BonusPetPerkStat::noRequiredMobType)
                .forEach(bonusPetPerkStat -> {
                    // Handle Stats
                    applyPetPercentage(this, null, variables, bonusPetPerkStat);

                    // Handle Armor
                    this.getArmor()
                        .stream()
                        .flatMap(Optional::stream)
                        .forEach(itemData -> applyPetPercentage(itemData, itemData.getCompoundTag(), variables, bonusPetPerkStat));

                    // Handle Accessories
                    this.getAccessoryBag()
                        .getAccessories()
                        .forEach(accessoryData -> applyPetPercentage(accessoryData, accessoryData.getCompoundTag(), variables, bonusPetPerkStat));
                });

            // TODO: Load Post Bonus Stats
        }
    }

    /**
     * Totals one member's stats, evaluating every bonus that depends on those totals.
     *
     * @param skyBlockIsland the profile the member belongs to, read for the shared bank balance
     * @param member the member to total
     * @return the member's stats
     */
    public static @NotNull ProfileStats compute(@NotNull SkyBlockIsland skyBlockIsland, @NotNull SkyBlockMember member) {
        return compute(skyBlockIsland, member, true);
    }

    /**
     * Totals one member's stats, optionally stopping before the bonuses that depend on those totals.
     * <p>
     * Every reference table this reads is resolved through a repository, so a connected session is
     * required and the work is not cheap.
     *
     * @param skyBlockIsland the profile the member belongs to, read for the shared bank balance
     * @param member the member to total
     * @param calculateBonusStats whether to evaluate the bonuses that depend on the flat totals
     * @return the member's stats
     */
    public static @NotNull ProfileStats compute(@NotNull SkyBlockIsland skyBlockIsland, @NotNull SkyBlockMember member, boolean calculateBonusStats) {
        return new ProfileStats(skyBlockIsland, member, calculateBonusStats);
    }

    /**
     * Conditional bonuses declared for the active pet's perks, gathered as the pet is read.
     */
    public ConcurrentList<BonusPetPerkStat> getBonusPetPerkStatModels() {
        return this.context.getBonusPetPerkStats();
    }

    /**
     * The player state a bonus expression can refer to, with the current stat totals folded in.
     * <p>
     * A fresh copy is built on every call, since the totals move as sources are added and a bonus
     * has to see the values as they stand when it runs.
     */
    public ConcurrentMap<String, Double> getExpressionVariables() {
        ConcurrentMap<String, Double> expressionVariables = Concurrent.newMap(this.context.getVariables());
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
        ConcurrentLinkedMap<Stat, Data> totalStats = this.reference.getStats()
            .stream()
            .map(statModel -> Pair.of(statModel, new Data()))
            .collect(Concurrent.toLinkedMap());

        // Collect Stat Data
        collectInto(totalStats, this, optimizerConstant);

        // Collect Accessory Data
        this.getAccessoryBag()
            .getAccessories()
            .forEach(accessoryData -> collectInto(totalStats, accessoryData, optimizerConstant));

        // Collect Armor Data
        this.getArmor()
            .stream()
            .flatMap(Optional::stream)
            .forEach(itemData -> collectInto(totalStats, itemData, optimizerConstant));

        return totalStats;
    }

    @Override
    protected StatSource[] getAllTypes() {
        return StatSource.values();
    }

    private static void collectInto(@NotNull ConcurrentLinkedMap<Stat, Data> totalStats, @NotNull StatData<?> statData, boolean optimizerConstant) {
        statData.getStats()
            .stream()
            .filter(entry -> !optimizerConstant || entry.getKey().isOptimizerConstant())
            .forEach(entry -> entry.getValue().forEach((statModel, data) -> {
                for (StatHalf half : StatHalf.values())
                    half.add(totalStats.get(statModel), half.read(data));
            }));
    }

    private static void applyPetPercentage(@NotNull StatData<?> statData, @Nullable CompoundTag compoundTag, @NotNull ConcurrentMap<String, Double> expressionVariables, @NotNull BonusPetPerkStat bonusPetPerkStat) {
        statData.getStats().forEach((origin, statEntries) -> statEntries.forEach((statModel, data) -> {
            for (StatHalf half : StatHalf.values())
                half.set(data, PlayerDataHelper.handleBonusEffects(statModel, half.read(data), compoundTag, expressionVariables, bonusPetPerkStat));
        }));
    }

    private void loadArmor(ReferenceSnapshot reference, SkyBlockMember member) {
        if (member.getInventory().getArmor() != null) {
            ConcurrentList<Pair<CompoundTag, Optional<Item>>> armorItemModels = member.getInventory().getArmor()
                .getNbtData()
                .<CompoundTag>getListTag("i")
                .stream()
                .map(itemTag -> Pair.of(
                    itemTag,
                    reference.getItem(itemTag.getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY).getValue())
                ))
                .collect(Concurrent.toList())
                .reversed();

            this.bonusArmorSetModel = reference.getBonusArmorSets().findFirst(
                Pair.of(BonusArmorSet::getHelmetItem, armorItemModels.getFirst().right().orElse(null)),
                Pair.of(BonusArmorSet::getChestplateItem, armorItemModels.get(1).right().orElse(null)),
                Pair.of(BonusArmorSet::getLeggingsItem, armorItemModels.get(2).right().orElse(null)),
                Pair.of(BonusArmorSet::getBootsItem, armorItemModels.get(3).right().orElse(null))
            );

            armorItemModels.forEach(armorItemModelPair -> {
                ItemData itemData = null;

                if (armorItemModelPair.left().notEmpty() && armorItemModelPair.right().isPresent())
                    itemData = new ItemData(
                        reference,
                        armorItemModelPair.right().get(),
                        armorItemModelPair.left()
                    );

                this.armor.add(Optional.ofNullable(itemData));
            });
        }
    }

}
