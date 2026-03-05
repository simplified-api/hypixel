package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.dungeon;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.ConcurrentSet;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.common.Weight;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.date.SkyBlockDate;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Getter
@NoArgsConstructor
public class DungeonProfile implements PostInit {

    @SerializedName("dungeon_types")
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, FloorData> dungeonMap = Concurrent.newMap();
    @SerializedName("player_classes")
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<DungeonClass.Type, ConcurrentMap<String, Double>> classMap = Concurrent.newMap();
    @SerializedPath("dungeon_journal.unlocked_journals")
    private @NotNull ConcurrentList<Integer> unlockedJournals = Concurrent.newList();
    @SerializedName("dungeons_blah_blah")
    private @NotNull ConcurrentSet<String> dungeonsFirstTalk = Concurrent.newSet();
    @SerializedName("selected_dungeon_class")
    private @NotNull DungeonClass.Type selectedClass = DungeonClass.Type.UNKNOWN;
    @SerializedName("daily_runs")
    private @NotNull DailyRuns dailyRuns = new DailyRuns();
    private @NotNull Treasures treasures = new Treasures();

    // PostInit

    private transient @NotNull ConcurrentMap<DungeonEntry.Type, DungeonEntry> dungeons = Concurrent.newMap();
    private transient @NotNull ConcurrentMap<DungeonClass.Type, DungeonClass> classes = Concurrent.newMap();

    @Override
    public void postInit() {
        this.dungeons = this.dungeonMap.stream()
            .filterKey(key -> !key.startsWith("MASTER_"))
            .mapKey(DungeonEntry.Type::of)
            .map((type, value) -> Pair.of(type, new DungeonEntry(
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

    public @NotNull DungeonEntry getDungeon(@NotNull DungeonEntry.Type dungeonType) {
        return this.getDungeons()
            .stream()
            .filterKey(type -> type == dungeonType)
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow();
    }

    public @NotNull ConcurrentMap<DungeonEntry, Weight> getWeight() {
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

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DailyRuns {

        @SerializedName("current_day_stamp")
        private int currentDayStamp;
        @SerializedName("completed_runs_count")
        private int completedRuns;

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Treasures {

        private @NotNull ConcurrentList<Run> runs = Concurrent.newList();
        private @NotNull ConcurrentList<Chest> chests = Concurrent.newList();

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Run {

            @SerializedName("run_id")
            private UUID id;
            @SerializedName("completion_ts")
            private SkyBlockDate.RealTime completionTime;
            @SerializedName("dungeon_type")
            private @NotNull DungeonEntry.Type dungeonType = DungeonEntry.Type.UNKNOWN;
            @SerializedName("dungeon_tier")
            private int tier;
            private @NotNull ConcurrentList<Participant> participants = Concurrent.newList();

            @Getter
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Participant {

                private static final Pattern DISPLAY_PATTERN = Pattern.compile(String.format(
                    "^%s([0-9a-f])(.*?)%<s[0-9a-f]: %<s[0-9a-f](.*?)%<s[0-9a-f] \\(%<s[0-9a-f]([0-9]+)%<s[0-9a-f]\\)",
                    ChatFormat.SECTION_SYMBOL
                ));

                @SerializedName("player_uuid")
                private UUID playerId;
                @SerializedName("display_name")
                private String displayName;
                @SerializedName("class_milestone")
                private int milestone;

                public int getClassLevel() {
                    return Integer.parseInt(DISPLAY_PATTERN.matcher(this.getDisplayName()).group(4));
                }

                public @NotNull DungeonClass.Type getClassType() {
                    return DungeonClass.Type.of(DISPLAY_PATTERN.matcher(this.getDisplayName()).group(3).toUpperCase());
                }

                public @NotNull String getName() {
                    return DISPLAY_PATTERN.matcher(this.getDisplayName()).group(2);
                }

            }

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Chest {

            @SerializedName("run_id")
            private @NotNull UUID runId;
            @SerializedName("chest_id")
            private @NotNull UUID chestId;
            @SerializedName("treasure_type")
            private @NotNull Type type;
            private int quality;
            @SerializedName("shiny_eligible")
            private boolean shinyEligible;
            private boolean paid;
            private int rerolls;
            @SerializedPath("rewards.rewards")
            private @NotNull ConcurrentList<String> items = Concurrent.newList();
            @SerializedPath("rewards.rolled_rng_meter_randomly")
            private boolean rolledRngMeterRandomly;

            public enum Type {

                WOOD,
                GOLD,
                DIAMOND,
                EMERALD,
                OBSIDIAN,
                BEDROCK

            }

        }

    }

}
