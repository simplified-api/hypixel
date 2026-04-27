package dev.sbs.hypixelapi.profile_stats.data;

import dev.sbs.skyblockdata.SkyBlockData;
import dev.sbs.skyblockdata.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.linked.ConcurrentLinkedMap;
import dev.simplified.collection.tuple.pair.Pair;
import lombok.Getter;

import java.util.Arrays;

@Getter
public abstract class StatData<T extends ObjectData.Type> {

    protected final ConcurrentMap<T, ConcurrentLinkedMap<Stat, Data>> stats = Concurrent.newMap();

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

    protected abstract T[] getAllTypes();

    @SafeVarargs
    public final Data getData(Stat statModel, T... types) {
        return this.getStatsOf(types).get(statModel);
    }

    public final ConcurrentLinkedMap<Stat, Data> getAllStats() {
        return this.getStatsOf(this.getAllTypes());
    }

    public final ConcurrentLinkedMap<Stat, Data> getStats(T type) {
        return this.stats.get(type);
    }

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
