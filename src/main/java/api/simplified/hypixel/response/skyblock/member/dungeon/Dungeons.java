package api.simplified.hypixel.response.skyblock.member.dungeon;

import api.simplified.hypixel.common.Weight;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NamingStyle;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.ConcurrentSet;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.gson.annotation.Lenient;
import dev.simplified.gson.annotation.SerializedPath;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A member's whole dungeon record.
 * <p>
 * Catacombs and master mode progress, the five class masteries, the reward chests Croesus is still
 * holding, and the Dungeon Hub odds and ends. Dungeons are PvE raids for a party of one to five
 * players; the Catacombs is the only one released.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Dungeons">Dungeons</a>
 */
@Getter
public class Dungeons {

    /**
     * Prefix marking the master mode half of a dungeon type, matched after the wire's keys have been
     * uppercased.
     */
    private static final @NotNull String MASTER_PREFIX = "MASTER_";

    /**
     * Shared stand-in for a class the wire named none of, carrying no experience.
     */
    private static final @NotNull DungeonClass EMPTY_CLASS = new DungeonClass(0);

    /**
     * Shared stand-in for a dungeon the wire named none of, carrying two empty floor records.
     */
    private static final @NotNull DungeonData EMPTY_DUNGEON = new DungeonData(new FloorData(), new FloorData());

    /**
     * Raw floor records keyed by the wire's lowercase dungeon type name, both difficulties still
     * loose beside each other.
     * <p>
     * It has no accessor of its own - anything after a floor goes through {@link #getDungeons()},
     * which is where the two halves are paired.
     */
    @SerializedName("dungeon_types")
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, FloorData> dungeonMap = Concurrent.newMap();

    /**
     * Class mastery for each dungeon class the wire named.
     */
    @SerializedName("player_classes")
    private @NotNull ConcurrentMap<DungeonClass.Type, DungeonClass> classes = Concurrent.newMap();

    /**
     * Journal pages the member has picked up in the Catacombs, read from a nested wire node.
     * <p>
     * The wire sends these as strings against a declared list of numbers, so every entry falls into
     * overflow, the field itself binds empty, and the array comes back verbatim on write.
     */
    @Lenient
    @SerializedPath("dungeon_journal.unlocked_journals")
    private @NotNull ConcurrentList<Integer> unlockedJournals = Concurrent.newList();

    /**
     * Ids of the Dungeon Hub and Catacombs Entrance NPCs the member has spoken to for the first
     * time.
     * <p>
     * The wire key really is spelled {@code dungeons_blah_blah} - an upstream placeholder that
     * shipped.
     */
    @SerializedName("dungeons_blah_blah")
    private @NotNull ConcurrentSet<String> dungeonsFirstTalk = Concurrent.newSet();

    /**
     * The class the member last queued as.
     */
    @SerializedName("selected_dungeon_class")
    private @NotNull DungeonClass.Type selectedClass = DungeonClass.Type.UNKNOWN;

    /**
     * The day-scoped counter of completed runs.
     */
    @SerializedName("daily_runs")
    private @NotNull DungeonDailies dailies = new DungeonDailies();

    /**
     * Completed runs Croesus is still holding chests for, read from the nested {@code treasures}
     * node.
     */
    @SerializedPath("treasures.runs")
    private @NotNull ConcurrentList<DungeonRun> runs = Concurrent.newList();

    /**
     * Unclaimed reward chests from those runs, read from the same nested {@code treasures} node the
     * runs are.
     */
    @SerializedPath("treasures.chests")
    private @NotNull ConcurrentList<DungeonChest> chests = Concurrent.newList();

    /**
     * Lifetime count of dungeon secrets found across every run.
     */
    private int secrets;

    /**
     * The floor the member last entered, as an opaque wire string such as
     * {@code CATACOMBS_FLOOR_SEVEN}.
     * <p>
     * Uppercase, unlike the lowercase dungeon type keys, and deliberately left a string rather than
     * an enum.
     */
    @SerializedName("last_dungeon_run")
    private @NotNull Optional<String> lastRun = Optional.empty();

    /**
     * Which Dungeon Hub race the member has armed and how it is configured.
     */
    @SerializedName("dungeon_hub_race_settings")
    private @NotNull RaceSettings raceSettings = new RaceSettings();

    @Getter(AccessLevel.NONE)
    private transient ConcurrentMap<DungeonData.Type, DungeonData> dungeons;

    /**
     * Dungeons keyed by type, each pairing the record of that type with its master mode counterpart.
     * <p>
     * Derived from the raw wire map and memoised - the first call computes it and every later call
     * hands back the same unmodifiable map. Every key is uppercased first, because the wire spells
     * both halves lowercase and a case-sensitive prefix test would let {@code master_catacombs}
     * through as a dungeon of its own and leave the real master floor unpaired. A dungeon the wire
     * sent no master half for is paired with an empty record rather than null.
     */
    public @NotNull ConcurrentMap<DungeonData.Type, DungeonData> getDungeons() {
        if (this.dungeons == null) {
            ConcurrentMap<String, FloorData> floors = this.dungeonMap.stream()
                .mapKey(key -> key.toUpperCase(Locale.ROOT))
                .toMap();

            this.dungeons = floors.stream()
                .filterKey(key -> !key.startsWith(MASTER_PREFIX))
                .mapKey(key -> DungeonData.Type.findByName(key).orElse(DungeonData.Type.UNKNOWN))
                .map((type, floorData) -> Pair.of(type, new DungeonData(
                    floorData,
                    floors.getOrDefault(MASTER_PREFIX + type.name(), new FloorData())
                )))
                .collect(Concurrent.toUnmodifiableMap());
        }

        return this.dungeons;
    }

    /**
     * Reads one class mastery.
     * <p>
     * This one-argument overload sits beside {@link Object#getClass()} - a no-argument call is still
     * that method and not this one.
     *
     * @param classType the class to read
     * @return that class's mastery, or a shared empty mastery when the wire named no such class
     */
    public @NotNull DungeonClass getClass(@NotNull DungeonClass.Type classType) {
        return this.getClasses().getOrDefault(classType, EMPTY_CLASS);
    }

    /**
     * Reads one dungeon by type.
     *
     * @param dungeonType the dungeon to read
     * @return that dungeon, or a shared empty dungeon when the wire named no such type
     */
    public @NotNull DungeonData getDungeon(@NotNull DungeonData.Type dungeonType) {
        return this.getDungeons().getOrDefault(dungeonType, EMPTY_DUNGEON);
    }

    /**
     * The {@link Weight} each dungeon contributes, keyed by the dungeon itself.
     * <p>
     * {@link DungeonData} declares no equality, so the keys match by identity - which holds only
     * because they come from the memoised map.
     */
    public @NotNull ConcurrentMap<DungeonData, Weight> getWeight() {
        return this.getDungeons()
            .stream()
            .map((type, dungeon) -> Pair.of(
                dungeon,
                dungeon.getWeight()
            ))
            .collect(Concurrent.toMap());
    }

    /**
     * Mean level over the classes the wire named, so one it never named cannot drag the average
     * down.
     */
    public double getClassAverage() {
        return this.getClasses()
            .stream()
            .map(Map.Entry::getValue)
            .mapToDouble(DungeonClass::getLevel)
            .average()
            .orElse(0.0);
    }

    /**
     * Class experience summed over every class the wire named.
     */
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

    @Getter
    public static class RaceSettings {

        @SerializedName("selected_race")
        private @NotNull Optional<String> selectedRace = Optional.empty();
        @SerializedName("selected_setting")
        private @NotNull Optional<String> selectedSetting = Optional.empty();
        @Getter(style = NamingStyle.FLUENT)
        private boolean runback;

    }

}
