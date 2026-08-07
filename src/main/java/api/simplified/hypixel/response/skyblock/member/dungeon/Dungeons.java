package api.simplified.hypixel.response.skyblock.member.dungeon;

import api.simplified.hypixel.common.Weight;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.ConcurrentSet;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.gson.PostInit;
import dev.simplified.gson.annotation.Lenient;
import dev.simplified.gson.annotation.SerializedPath;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Getter
public class Dungeons implements PostInit {

    private static final @NotNull String MASTER_PREFIX = "MASTER_";
    private static final @NotNull DungeonClass EMPTY_CLASS = new DungeonClass(0);
    private static final @NotNull DungeonData EMPTY_DUNGEON = new DungeonData(0, new FloorData(), new FloorData());

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
    private int secrets;
    @SerializedName("last_dungeon_run")
    private @NotNull Optional<String> lastRun = Optional.empty();
    @SerializedName("dungeon_hub_race_settings")
    private @NotNull RaceSettings raceSettings = new RaceSettings();

    // PostInit

    private transient @NotNull ConcurrentMap<DungeonData.Type, DungeonData> dungeons = Concurrent.newMap();
    private transient @NotNull ConcurrentMap<DungeonClass.Type, DungeonClass> classes = Concurrent.newMap();

    @Override
    public void postInit() {
        // the wire spells these keys lowercase and a master-mode floor is found by prefixing a constant
        // name, so the key space is normalised once rather than compared in two different cases
        ConcurrentMap<String, FloorData> floors = this.dungeonMap.stream()
            .mapKey(key -> key.toUpperCase(Locale.ROOT))
            .toMap();

        this.dungeons = floors.stream()
            .filterKey(key -> !key.startsWith(MASTER_PREFIX))
            .mapKey(DungeonData.Type::of)
            .map((type, floorData) -> Pair.of(type, new DungeonData(
                floorData.getExperience(),
                floorData,
                floors.getOrDefault(MASTER_PREFIX + type.name(), new FloorData())
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
        return this.getClasses().getOrDefault(classType, EMPTY_CLASS);
    }

    public @NotNull DungeonData getDungeon(@NotNull DungeonData.Type dungeonType) {
        return this.getDungeons().getOrDefault(dungeonType, EMPTY_DUNGEON);
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

    @Getter
    public static class RaceSettings {

        @SerializedName("selected_race")
        private @NotNull Optional<String> selectedRace = Optional.empty();
        @SerializedName("selected_setting")
        private @NotNull Optional<String> selectedSetting = Optional.empty();
        @Accessors(fluent = true)
        private boolean runback;

    }

}
