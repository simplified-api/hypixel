package api.simplified.hypixel.profile_stats.data;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentLinkedMap;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import lombok.Getter;

import java.util.Arrays;

/**
 * A source of stats that keeps its contribution split by where each part came from.
 * <p>
 * Every subclass names its own set of buckets, so an accessory can report what its gemstones gave
 * separately from what its enrichment gave, and a caller can total whichever subset it cares about.
 * Reading any total resolves the stat list from a repository, so a session is needed.
 *
 * @param <T> the bucket type this source splits its stats by
 */
@Getter
public abstract class StatData<T extends ObjectData.Type> {

    /**
     * Every stat this source provides, bucketed by where the value came from.
     */
    protected final ConcurrentMap<T, ConcurrentLinkedMap<Stat, Data>> stats = Concurrent.newMap();

    /**
     * Totals one stat across every bucket at once.
     *
     * @param statModel the stat to total
     * @return the summed base and bonus, both zero when no bucket provides it
     */
    public final Data getAllData(Stat statModel) {
        Data statData = new Data();

        this.stats.forEach((type, statEntries) -> statEntries.stream()
            .filter(statModelDataEntry -> statModelDataEntry.getKey().equals(statModel))
            .forEach(statModelDataEntry -> {
                statData.addBase(statModelDataEntry.getValue().getBase());
                statData.addBonus(statModelDataEntry.getValue().getBonus());
            }));

        return statData;
    }

    protected final void addBase(Data data, double value) {
        data.addBase(value);
    }

    protected final void addBonus(Data data, double value) {
        data.addBonus(value);
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
     * Reads one bucket on its own.
     *
     * @param type the bucket to read
     * @return the stats that bucket provides
     */
    public final ConcurrentLinkedMap<Stat, Data> getStats(T type) {
        return this.stats.get(type);
    }

    /**
     * Sums a chosen subset of buckets into one table.
     * <p>
     * The table is seeded from every stat in the repository, so a stat no bucket provides is present
     * at zero rather than absent - a caller can read any stat without checking first.
     *
     * @param types the buckets to include
     * @return a fresh table covering every known stat
     */
    @SafeVarargs
    public final ConcurrentLinkedMap<Stat, Data> getStatsOf(T... types) {
        ConcurrentLinkedMap<Stat, Data> totalStats = SkyBlockData.getRepository(Stat.class)
            .findAll()
            .stream()
            .map(statModel -> Pair.of(statModel, new Data()))
            .collect(Concurrent.toLinkedMap());

        Arrays.stream(types)
            .flatMap(type -> this.stats.get(type).stream())
            .forEach(entry -> {
                Data statData = totalStats.get(entry.getKey());
                statData.addBase(entry.getValue().getBase());
                statData.addBonus(entry.getValue().getBonus());
            });

        return totalStats;
    }

    protected final void setBase(Data data, double value) {
        data.base = value;
    }

    protected final void setBonus(Data data, double value) {
        data.bonus = value;
    }

}
