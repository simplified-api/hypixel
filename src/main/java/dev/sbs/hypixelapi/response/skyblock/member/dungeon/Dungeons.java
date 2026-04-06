package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.dungeon;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.common.Weight;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.ConcurrentSet;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.gson.Lenient;
import dev.simplified.gson.PostInit;
import dev.simplified.gson.SerializedPath;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
public class Dungeons implements PostInit {

    @SerializedName("dungeon_types")
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, FloorData> dungeonMap = Concurrent.newMap();
    @SerializedName("player_classes")
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<DungeonClass.Type, ConcurrentMap<String, Double>> classMap = Concurrent.newMap();
    @Lenient
    @SerializedPath("dungeon_journal.unlocked_journals")
    private @NotNull ConcurrentList<Integer> unlockedJournals = Concurrent.newList();
    @SerializedName("dungeons_blah_blah")
    private @NotNull ConcurrentSet<String> dungeonsFirstTalk = Concurrent.newSet();
    @SerializedName("selected_dungeon_class")
    private @NotNull DungeonClass.Type selectedClass = DungeonClass.Type.UNKNOWN;
    @SerializedName("daily_runs")
    private @NotNull DungeonDailies dailies = new DungeonDailies();
    @Getter(AccessLevel.NONE)
    private @NotNull DungeonTreasures treasures = new DungeonTreasures();

    // PostInit

    private transient @NotNull ConcurrentMap<DungeonData.Type, DungeonData> dungeons = Concurrent.newMap();
    private transient @NotNull ConcurrentMap<DungeonClass.Type, DungeonClass> classes = Concurrent.newMap();

    @Override
    public void postInit() {
        this.dungeons = this.dungeonMap.stream()
            .filterKey(key -> !key.startsWith("MASTER_"))
            .mapKey(DungeonData.Type::of)
            .map((type, value) -> Pair.of(type, new DungeonData(
                value.getExperience(),
                value,
                this.dungeonMap.getOrDefault(
                    String.format("MASTER_%s", type.name()),
                    new FloorData()
                )
            )))
            .collect(Concurrent.toUnmodifiableMap());

        this.classes = this.classMap.stream()
            .map(entry -> Pair.of(
                entry.getKey(),
                new DungeonClass(entry.getValue().get("experience"))
            ))
            .collect(Concurrent.toUnmodifiableMap());
    }

    public @NotNull DungeonClass getClass(@NotNull DungeonClass.Type classType) {
        return this.getClasses()
            .stream()
            .filterKey(type -> type == classType)
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow();
    }

    public @NotNull DungeonData getDungeon(@NotNull DungeonData.Type dungeonType) {
        return this.getDungeons()
            .stream()
            .filterKey(type -> type == dungeonType)
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow();
    }

    public @NotNull ConcurrentMap<DungeonData, Weight> getWeight() {
        return this.getDungeons()
            .stream()
            .map((type, dungeon) -> Pair.of(
                dungeon,
                dungeon.getWeight()
            ))
            .collect(Concurrent.toMap());
    }

    public double getClassAverage() {
        return this.getClasses()
            .stream()
            .map(Map.Entry::getValue)
            .mapToDouble(DungeonClass::getLevel)
            .average()
            .orElse(0.0);
    }

    public double getClassExperience() {
        return this.getClasses()
            .stream()
            .map(Map.Entry::getValue)
            .mapToDouble(DungeonClass::getExperience)
            .sum();
    }

    public double getClassProgressPercentage() {
        return this.getClasses()
            .stream()
            .map(Map.Entry::getValue)
            .mapToDouble(DungeonClass::getTotalProgressPercentage)
            .average()
            .orElse(0.0);
    }

    public @NotNull ConcurrentMap<DungeonClass, Weight> getClassWeight() {
        return this.getClasses()
            .stream()
            .map((type, dungeonClass) -> Pair.of(
                dungeonClass,
                dungeonClass.getWeight()
            ))
            .collect(Concurrent.toMap());
    }

    public @NotNull ConcurrentList<DungeonChest> getChests() {
        return this.treasures.getChests();
    }

    public @NotNull ConcurrentList<DungeonRun> getRuns() {
        return this.treasures.getRuns();
    }

    @Getter
    private static class DungeonTreasures {

        private @NotNull ConcurrentList<DungeonRun> runs = Concurrent.newList();
        private @NotNull ConcurrentList<DungeonChest> chests = Concurrent.newList();

    }

}
