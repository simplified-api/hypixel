package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.SkyBlockIsland;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import api.simplified.hypixel.response.skyblock.member.AccessoryBag;
import api.simplified.hypixel.response.skyblock.member.pet.OwnedPet;
import api.simplified.hypixel.response.skyblock.stats.buff.BuffEvaluator;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentLinkedMap;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Every stat one member has on one profile, totalled from all eighteen sources that feed them.
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
public class ProfileStats {

    private static final @NotNull ConcurrentList<StatSource> EVERY_SOURCE = Concurrent.newUnmodifiableList(StatSource.values());

    /**
     * The member's accessory bag, already initialized with its member-scoped values.
     */
    private final AccessoryBag accessoryBag;

    /**
     * The pet currently summoned, empty when none is.
     */
    private final Optional<OwnedPet> activePet;

    @Getter(AccessLevel.NONE)
    private final StatContext context;

    @Getter(AccessLevel.NONE)
    private final StatSheet sheet = new StatSheet();

    @Getter(AccessLevel.NONE)
    private final ConcurrentMap<String, Double> variables = Concurrent.newMap();

    @Getter(AccessLevel.NONE)
    private final ConcurrentList<StatCap.Scoped> caps;

    private ProfileStats(@NotNull SkyBlockIsland skyBlockIsland, @NotNull SkyBlockMember member, boolean calculateBonusStats, @NotNull ConcurrentList<StatSource> sources) {
        this.activePet = member.getPets().getActivePet();
        this.accessoryBag = member.getAccessoryBag();
        this.context = new StatContext(skyBlockIsland, member);

        // --- Variable Seed ---
        // every provider writes through a sink it cannot read back, so the seed has no order either
        for (VariableProvider provider : VariableProvider.values())
            provider.provide(this.context, this.variables::put);

        // --- Flat Pass ---
        sources.forEach(source -> source.contribute(this.context, this.sheet));
        this.sheet.getProfile().publishTotals(this.variables::put);

        // each source's own contribution is named as well, off the cells it wrote rather than
        // part-way through writing them - the pet's among them, which is what a rule scaling a stat
        // by what the pet gave for it reads
        this.sheet.getProfile().publishOriginTotals(this.variables::put);

        // --- Bonus Pass ---
        // the sheet is filled by now, which the group counts need - a count taken before the flat
        // pass opened the slots is a count of nothing
        ConcurrentList<BuffEvaluator> program = PostProcess.program(this.context);
        ComputeFacts facts = ComputeFacts.of(this.context, this.sheet);

        if (calculateBonusStats)
            PostProcess.run(this.sheet, this.variables, program, facts);

        // --- Caps ---
        // resolved once the sheet is settled and never written back into it, because a ceiling is a
        // property of reading sources together rather than a contribution any one of them made
        this.caps = PostProcess.capProgram(this.sheet, this.variables, program, facts);
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
     * The accessories that count toward magical power, each totalled in its own right.
     */
    public @NotNull ConcurrentList<ItemStack> getAccessories() {
        return this.stacks(ItemSlot.Kind.ACCESSORY);
    }

    /**
     * Totals one stat across every source at once.
     *
     * @param statModel the stat to total
     * @return the summed base and bonus, both zero when no source provides it
     */
    public Data getAllData(Stat statModel) {
        return this.getAllStats().getOrDefault(statModel, new Data());
    }

    /**
     * Every stat the profile's own sources provide, summed across all of them.
     */
    public ConcurrentLinkedMap<Stat, Data> getAllStats() {
        return this.sheet.getProfile().toMap();
    }

    /**
     * The worn armour pieces, each totalled in its own right.
     */
    public @NotNull ConcurrentList<ItemStack> getArmor() {
        return this.stacks(ItemSlot.Kind.ARMOR);
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

        // Collect Profile Data
        collectInto(totalStats, this.sheet.getProfile(), optimizerConstant);

        // Collect Item Data
        this.sheet.getSlots().forEach((address, slot) -> collectInto(totalStats, slot.table(), optimizerConstant));

        // Clamp
        // the ceiling reaches the member's whole number, so it applies here and not to the cells this
        // summed - which is why a clamped read leaves the sheet reading exactly what each source gave
        totalStats.forEach((statModel, data) -> {
            StatCap cap = this.getStatCap(statModel);

            if (cap.isUncapped())
                return;

            double clamped = cap.clamp(data.getTotal());

            // the base half is what the member has before anything was added to it, so an overflow is
            // taken off the bonus and the base is left alone
            if (clamped < data.getTotal())
                StatHalf.BONUS.set(data, clamped - data.getBase());
        });

        return totalStats;
    }

    /**
     * One stat's ceiling as it stands for this member.
     *
     * @param statModel the stat to read
     * @return the resolved ceiling, uncapped when nothing clamps it
     */
    public @NotNull StatCap getStatCap(@NotNull Stat statModel) {
        return StatCap.resolve(statModel, this.caps);
    }

    /**
     * Damage scaling earned from combat levels, as a fraction rather than a percentage.
     */
    public double getDamageMultiplier() {
        return this.variables.getOrDefault(VariableProvider.DAMAGE_MULTIPLIER.name(), 0.0);
    }

    /**
     * Reads one stat from a chosen subset of sources.
     *
     * @param statModel the stat to read
     * @param sources the sources to include
     * @return the summed base and bonus across those sources
     */
    public Data getData(Stat statModel, StatSource... sources) {
        return this.getStatsOf(sources).get(statModel);
    }

    /**
     * Every source that provided something, with the stats it provided.
     * <p>
     * A source that provided nothing is absent rather than present and empty, and so is a stat no
     * source wrote - reading a total through {@link #getStatsOf} seeds those back at zero.
     */
    public ConcurrentMap<StatOrigin, ConcurrentLinkedMap<Stat, Data>> getStats() {
        return this.sheet.getProfile().getEntries();
    }

    /**
     * Reads one source on its own.
     *
     * @param source the source to read
     * @return the stats that source provides
     */
    public ConcurrentLinkedMap<Stat, Data> getStats(StatSource source) {
        return this.sheet.getProfile().getEntries(source);
    }

    /**
     * Sums a chosen subset of sources into one table.
     * <p>
     * The table is seeded from every stat in the reference data, so a stat no source provides is
     * present at zero rather than absent - a caller can read any stat without checking first.
     *
     * @param sources the sources to include
     * @return a fresh table covering every known stat
     */
    public ConcurrentLinkedMap<Stat, Data> getStatsOf(StatSource... sources) {
        return this.sheet.getProfile().toMap(sources);
    }

    /**
     * Everything this compute wrote: the profile's own table and one per filled item slot.
     */
    @NotNull StatSheet getSheet() {
        return this.sheet;
    }

    private static void collectInto(@NotNull ConcurrentLinkedMap<Stat, Data> totalStats, @NotNull StatTable table, boolean optimizerConstant) {
        table.getEntries()
            .stream()
            .filter(entry -> !optimizerConstant || entry.getKey().isOptimizerConstant())
            .forEach(entry -> entry.getValue().forEach((statModel, data) -> {
                for (StatHalf half : StatHalf.values())
                    half.add(totalStats.get(statModel), half.read(data));
            }));
    }

    private @NotNull ConcurrentList<ItemStack> stacks(@NotNull ItemSlot.Kind kind) {
        return this.sheet.getSlots(kind)
            .stream()
            .map(StatSheet.Slot::stack)
            .collect(Concurrent.toUnmodifiableList());
    }

}
