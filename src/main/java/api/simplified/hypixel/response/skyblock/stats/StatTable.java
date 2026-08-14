package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentLinkedMap;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Every stat one owner contributes, kept per origin and holding only the cells something wrote.
 * <p>
 * A cell no source touches reads zero, so storing it is storing a zero. The dense shape this replaces
 * seeded one value per bucket per stat before any source ran, which for a profile is sixteen buckets
 * across every known stat and for each item another six.
 * <p>
 * Reads that a consumer sees are unaffected: {@link #toMap} seeds every known stat, so a caller may
 * still ask for one that nothing provided and read zero rather than checking first.
 */
final class StatTable {

    private final @NotNull ConcurrentMap<StatOrigin, ConcurrentLinkedMap<Stat, Data>> entries = Concurrent.newMap();

    /**
     * Stat ids no {@link Stat} row matches, counted once per id rather than dropped in silence.
     */
    @Getter private final @NotNull ConcurrentMap<String, Integer> unresolvedStatIds = Concurrent.newMap();

    /**
     * Adds one value to a stat already in hand.
     *
     * @param origin where the value came from
     * @param statModel the stat the value is for
     * @param half which half of the value to write
     * @param value the amount to add
     * @return this table
     */
    public @NotNull StatTable add(@NotNull StatOrigin origin, @NotNull Stat statModel, @NotNull StatHalf half, double value) {
        half.add(this.getCell(origin, statModel), value);
        return this;
    }

    /**
     * Adds one value to the stat of a given id, counting the id when no row carries it.
     *
     * @param origin where the value came from
     * @param statId the id of the stat the value is for
     * @param half which half of the value to write
     * @param value the amount to add
     * @return this table
     */
    public @NotNull StatTable add(@NotNull StatOrigin origin, @NotNull String statId, @NotNull StatHalf half, double value) {
        return SkyBlockData.getRepository(Stat.class).findFirst(Stat::getId, statId)
            .map(statModel -> this.add(origin, statModel, half, value))
            .orElseGet(() -> {
                this.unresolvedStatIds.merge(statId, 1, Integer::sum);
                return this;
            });
    }

    /**
     * One origin's cell for one stat, created empty the first time it is asked for.
     *
     * @param origin the origin to write under
     * @param statModel the stat to write
     * @return the live cell
     */
    public @NotNull Data getCell(@NotNull StatOrigin origin, @NotNull Stat statModel) {
        return this.entries.computeIfAbsent(origin, key -> Concurrent.newLinkedMap())
            .computeIfAbsent(statModel, key -> new Data());
    }

    /**
     * Every origin that wrote something, with the cells it wrote.
     * <p>
     * The cells are live, so a pass that rescales what is already there writes through this view.
     *
     * @return the written cells, keyed by origin
     */
    public @NotNull ConcurrentMap<StatOrigin, ConcurrentLinkedMap<Stat, Data>> getEntries() {
        return this.entries;
    }

    /**
     * What one origin wrote, empty when it wrote nothing.
     *
     * @param origin the origin to read
     * @return the cells that origin wrote
     */
    public @NotNull ConcurrentLinkedMap<Stat, Data> getEntries(@NotNull StatOrigin origin) {
        return this.entries.getOrDefault(origin, Concurrent.newLinkedMap());
    }

    /**
     * Rewrites every cell something wrote, which is what a multiplier pass does.
     * <p>
     * An unwritten cell is zero and stays absent - rescaling it would write a zero, and folding it
     * through a rule that adds would invent a contribution no source made.
     *
     * @param rewriter given the stat, the half and the number as it stands, answers the new number
     */
    public void rewrite(@NotNull Rewriter rewriter) {
        this.entries.forEach((origin, statEntries) -> statEntries.forEach((statModel, data) -> {
            for (StatHalf half : StatHalf.values())
                half.set(data, rewriter.rewrite(statModel, half, half.read(data)));
        }));
    }

    /**
     * Rewrites every written cell of one stat, across every origin that wrote it.
     *
     * @param target the stat to rewrite
     * @param rewriter given the stat, the half and the number as it stands, answers the new number
     */
    public void rewrite(@NotNull Stat target, @NotNull Rewriter rewriter) {
        this.entries.forEach((origin, statEntries) -> {
            Data data = statEntries.get(target);

            if (data == null)
                return;

            for (StatHalf half : StatHalf.values())
                half.set(data, rewriter.rewrite(target, half, half.read(data)));
        });
    }

    /**
     * Adds every cell a chosen subset of origins wrote into a reading already keyed by stat.
     *
     * @param totals the reading to add into
     * @param origins the origins to include
     */
    public void addTo(@NotNull ConcurrentLinkedMap<Stat, Data> totals, @NotNull StatOrigin... origins) {
        Arrays.stream(origins)
            .flatMap(origin -> this.getEntries(origin).stream())
            .forEach(entry -> {
                Data target = totals.get(entry.getKey());
                target.base += entry.getValue().getBase();
                target.bonus += entry.getValue().getBonus();
            });
    }

    /**
     * Sums a chosen subset of origins into one table, seeded across every known stat so an untouched
     * stat reads zero rather than being absent.
     *
     * @param origins the origins to include
     * @return a fresh table covering every known stat
     */
    public @NotNull ConcurrentLinkedMap<Stat, Data> toMap(@NotNull StatOrigin... origins) {
        ConcurrentLinkedMap<Stat, Data> totals = this.seed();
        this.addTo(totals, origins);
        return totals;
    }

    /**
     * Sums every origin that wrote anything into one table, seeded across every known stat.
     *
     * @return a fresh table covering every known stat
     */
    public @NotNull ConcurrentLinkedMap<Stat, Data> toMap() {
        ConcurrentLinkedMap<Stat, Data> totals = this.seed();

        this.entries.forEach((origin, statEntries) -> statEntries.forEach((statModel, data) -> {
            Data target = totals.get(statModel);
            target.base += data.getBase();
            target.bonus += data.getBonus();
        }));

        return totals;
    }

    /**
     * Publishes every stat total under {@code STAT_<statId>}, so a later pass's expressions read the
     * pass that has just finished rather than one that is still running.
     *
     * @param sink where the variables are written
     */
    void publishTotals(@NotNull VariableSink sink) {
        this.toMap().forEach((statModel, data) -> sink.put(String.format("STAT_%s", statModel.getId()), data.getTotal()));
    }

    /**
     * Publishes what each origin gave on its own under {@code STAT_<origin>_<statId>}, so a rule can
     * name one source's contribution rather than the member's whole number.
     * <p>
     * Only a written cell is published. A stat an origin never touched is absent rather than zero,
     * which is what lets a rule reading one be left unevaluated instead of folding in a contribution
     * that origin never made.
     *
     * @param sink where the variables are written
     */
    void publishOriginTotals(@NotNull VariableSink sink) {
        this.entries.forEach((origin, statEntries) -> statEntries.forEach((statModel, data) -> sink.put(
            String.format("STAT_%s_%s", origin.name(), statModel.getId()),
            data.getTotal()
        )));
    }

    private @NotNull ConcurrentLinkedMap<Stat, Data> seed() {
        return SkyBlockData.getRepository(Stat.class).findAll()
            .stream()
            .map(statModel -> Pair.of(statModel, new Data()))
            .collect(Concurrent.toLinkedMap());
    }

    /**
     * Answers what one already-written number becomes.
     */
    @FunctionalInterface
    interface Rewriter {

        /**
         * Rewrites one number.
         *
         * @param statModel the stat the number belongs to
         * @param half which half the number is
         * @param current the number as it stands
         * @return the number to write back
         */
        double rewrite(@NotNull Stat statModel, @NotNull StatHalf half, double current);

    }

}
