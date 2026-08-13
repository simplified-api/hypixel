package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.SkyBlockIsland;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import api.simplified.hypixel.response.skyblock.member.AccessoryBag;
import api.simplified.hypixel.response.skyblock.member.pet.OwnedPet;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.BonusArmorSet;
import api.simplified.skyblock.model.BonusPetPerkStat;
import api.simplified.skyblock.model.Item;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentLinkedMap;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.tuple.pair.Pair;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.StringTag;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

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

    private static final @NotNull ConcurrentList<StatSource> EVERY_SOURCE = Concurrent.newUnmodifiableList(StatSource.values());

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
    private final ConcurrentList<Optional<ItemStack>> armor = Concurrent.newList();

    /**
     * The accessories that count toward magical power, each totalled in its own right.
     * <p>
     * These belong to the result rather than to the bag, so a second compute for the same member
     * starts from unwritten tables rather than from the first one's.
     */
    private final ConcurrentList<ItemStack> accessories = Concurrent.newList();

    /**
     * Set bonus the worn armour qualifies for, empty when the pieces do not form a set.
     */
    private Optional<BonusArmorSet> bonusArmorSetModel = Optional.empty();

    @Getter(AccessLevel.NONE)
    private final StatContext context;

    private ProfileStats(@NotNull SkyBlockIsland skyBlockIsland, @NotNull SkyBlockMember member, boolean calculateBonusStats, @NotNull ConcurrentList<StatSource> sources) {
        this.activePet = member.getPets().getActivePet();
        this.accessoryBag = member.getAccessoryBag();
        this.context = new StatContext(skyBlockIsland, member, this.accessoryBag);

        // --- Resolve Gear ---
        // neither is a source - each is a list of tables of its own - so both are resolved here rather
        // than reached for once a pass is running
        this.accessoryBag.getAccessories()
            .forEach(detectedAccessory -> this.accessories.add(new ItemStack(
                detectedAccessory.getAccessory().getItem(),
                Optional.of(detectedAccessory.getAccessory()),
                detectedAccessory.getCompoundTag()
            )));
        this.loadArmor(member);

        // --- Variable Seed ---
        // every provider writes through a sink it cannot read back, so the seed has no order either
        for (VariableProvider provider : VariableProvider.values())
            provider.provide(this.context, this.context.getVariables()::put);

        // --- Flat Pass ---
        sources.forEach(source -> source.contribute(this.context, this.table));
        this.table.publishTotals(this.context.getVariables()::put);

        // --- Bonus Pass ---
        if (calculateBonusStats)
            PostProcess.run(this.context, this.table, this.armor, this.accessories);
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
        return compute(skyBlockIsland, member, calculateBonusStats, EVERY_SOURCE);
    }

    /**
     * Totals one member's stats from a given set of sources, in the order given.
     * <p>
     * A source writes through a sink it cannot read back, so the flat pass has no order to get wrong
     * and no source can be made to depend on another having run. That is a property rather than an
     * observation, which is what this reaches far enough in to check.
     *
     * @param skyBlockIsland the profile the member belongs to, read for the shared bank balance
     * @param member the member to total
     * @param calculateBonusStats whether to evaluate the bonuses that depend on the flat totals
     * @param sources the sources to run, in the order to run them
     * @return the member's stats, holding only what those sources contribute
     */
    static @NotNull ProfileStats compute(@NotNull SkyBlockIsland skyBlockIsland, @NotNull SkyBlockMember member, boolean calculateBonusStats, @NotNull ConcurrentList<StatSource> sources) {
        return new ProfileStats(skyBlockIsland, member, calculateBonusStats, sources);
    }

    /**
     * Damage scaling earned from combat levels, as a fraction rather than a percentage.
     */
    public double getDamageMultiplier() {
        return this.context.getVariables().getOrDefault(VariableProvider.DAMAGE_MULTIPLIER.name(), 0.0);
    }

    /**
     * Conditional bonuses declared for the active pet's perks, gathered as the pet is read.
     */
    public ConcurrentList<BonusPetPerkStat> getBonusPetPerkStatModels() {
        return this.context.getBonusPetPerkStats();
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
        ConcurrentLinkedMap<Stat, Data> totalStats = SkyBlockData.getRepository(Stat.class).findAll()
            .stream()
            .map(statModel -> Pair.of(statModel, new Data()))
            .collect(Concurrent.toLinkedMap());

        // Collect Stat Data
        collectInto(totalStats, this, optimizerConstant);

        // Collect Accessory Data
        this.getAccessories()
            .forEach(accessoryStats -> collectInto(totalStats, accessoryStats, optimizerConstant));

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

    private void loadArmor(@NotNull SkyBlockMember member) {
        ConcurrentList<Pair<CompoundTag, Optional<Item>>> armorItemModels = member.getInventory().getArmor()
            .getNbtData()
            .<CompoundTag>getListTag("i")
            .stream()
            .map(itemTag -> Pair.of(
                itemTag,
                SkyBlockData.getRepository(Item.class).findFirst(Item::getId, itemTag.getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY).getValue())
            ))
            .collect(Concurrent.toList())
            .reversed();

        this.bonusArmorSetModel = SkyBlockData.getRepository(BonusArmorSet.class).findAll().findFirst(
            Pair.of(BonusArmorSet::getHelmetItem, armorItemModels.getFirst().right().orElse(null)),
            Pair.of(BonusArmorSet::getChestplateItem, armorItemModels.get(1).right().orElse(null)),
            Pair.of(BonusArmorSet::getLeggingsItem, armorItemModels.get(2).right().orElse(null)),
            Pair.of(BonusArmorSet::getBootsItem, armorItemModels.get(3).right().orElse(null))
        );

        armorItemModels.forEach(armorItemModelPair -> {
            ItemStack itemStats = null;

            if (armorItemModelPair.left().notEmpty() && armorItemModelPair.right().isPresent())
                itemStats = new ItemStack(
                    armorItemModelPair.right().get(),
                    Optional.empty(),
                    armorItemModelPair.left()
                );

            this.armor.add(Optional.ofNullable(itemStats));
        });
    }

}
