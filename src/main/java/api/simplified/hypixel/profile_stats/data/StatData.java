package api.simplified.hypixel.profile_stats.data;

import api.simplified.hypixel.profile_stats.ReferenceSnapshot;
import api.simplified.hypixel.profile_stats.StatOrigin;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.ConcurrentLinkedMap;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

/**
 * A source of stats that keeps its contribution split by where each part came from.
 * <p>
 * Every subclass names its own set of buckets, so an accessory can report what its gemstones gave
 * separately from what its enrichment gave, and a caller can total whichever subset it cares about.
 *
 * @param <T> the bucket type this source splits its stats by
 */
public abstract class StatData<T extends StatOrigin> {

    /**
     * Every stat this source provides, held per bucket and only for the cells something wrote.
     */
    protected final @NotNull StatTable table;

    /**
     * The reference tables this source resolves every id against.
     */
    protected final @NotNull ReferenceSnapshot reference;

    /**
     * Constructs a new {@code StatData} reading its reference data from one snapshot.
     *
     * @param reference the reference tables to resolve against
     */
    protected StatData(@NotNull ReferenceSnapshot reference) {
        this.reference = reference;
        this.table = new StatTable(reference);
    }

    /**
     * The table this source writes into, which a pass that rescales what is already there rewrites
     * through.
     */
    public final @NotNull StatTable getTable() {
        return this.table;
    }

    /**
     * Totals one stat across every bucket at once.
     *
     * @param statModel the stat to total
     * @return the summed base and bonus, both zero when no bucket provides it
     */
    public final Data getAllData(Stat statModel) {
        Data statData = new Data();

        this.table.getEntries().forEach((origin, statEntries) -> statEntries.stream()
            .filter(statModelDataEntry -> statModelDataEntry.getKey().equals(statModel))
            .forEach(statModelDataEntry -> {
                statData.base += statModelDataEntry.getValue().getBase();
                statData.bonus += statModelDataEntry.getValue().getBonus();
            }));

        return statData;
    }

    /**
     * Every bucket this source splits its stats by, normally the whole enum.
     *
     * @return the bucket constants
     */
    protected abstract T[] getAllTypes();

    /**
     * Reads one stat from a chosen subset of buckets.
     *
     * @param statModel the stat to read
     * @param types the buckets to include
     * @return the summed base and bonus across those buckets
     */
    @SafeVarargs
    public final Data getData(Stat statModel, T... types) {
        return this.getStatsOf(types).get(statModel);
    }

    /**
     * Every stat this source provides, summed across all of its buckets.
     */
    public final ConcurrentLinkedMap<Stat, Data> getAllStats() {
        return this.getStatsOf(this.getAllTypes());
    }

    /**
     * Every bucket that provided something, with the stats it provided.
     * <p>
     * A bucket that provided nothing is absent rather than present and empty, and so is a stat no
     * bucket wrote - reading a total through {@link #getStatsOf} seeds those back at zero.
     */
    public final ConcurrentMap<StatOrigin, ConcurrentLinkedMap<Stat, Data>> getStats() {
        return this.table.getEntries();
    }

    /**
     * Reads one bucket on its own.
     *
     * @param type the bucket to read
     * @return the stats that bucket provides
     */
    public final ConcurrentLinkedMap<Stat, Data> getStats(T type) {
        return this.table.getEntries(type);
    }

    /**
     * Sums a chosen subset of buckets into one table.
     * <p>
     * The table is seeded from every stat in the reference data, so a stat no bucket provides is
     * present at zero rather than absent - a caller can read any stat without checking first.
     *
     * @param types the buckets to include
     * @return a fresh table covering every known stat
     */
    @SafeVarargs
    public final ConcurrentLinkedMap<Stat, Data> getStatsOf(T... types) {
        return this.table.toMap(types);
    }

}
