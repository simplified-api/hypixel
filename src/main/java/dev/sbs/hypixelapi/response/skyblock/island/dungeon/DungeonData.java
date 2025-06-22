package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.dungeon;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.ConcurrentSet;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.minecraftapi.text.ChatFormat;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@NoArgsConstructor
public class DungeonData {

    @SerializedName("dungeon_types")
    private @NotNull ConcurrentMap<Dungeon.Type, Dungeon> dungeonMap = Concurrent.newMap();
    private transient @NotNull ConcurrentList<Dungeon> dungeonList = Concurrent.newList(); // Initialized Later
    @SerializedName("player_classes")
    private @NotNull ConcurrentMap<Dungeon.Class.Type, ConcurrentMap<String, Double>> classMap = Concurrent.newMap();
    private transient @NotNull ConcurrentList<Dungeon.Class> classList = Concurrent.newList(); // Initialized Later
    @SerializedPath("dungeon_journal.unlocked_journals")
    @Getter private @NotNull ConcurrentList<Integer> unlockedJournals = Concurrent.newList();
    @SerializedName("dungeons_blah_blah")
    @Getter private @NotNull ConcurrentSet<String> dungeonsFirstTalk = Concurrent.newSet();
    @SerializedName("selected_dungeon_class")
    @Getter private @NotNull Dungeon.Class.Type selectedClass;
    @SerializedName("daily_runs")
    @Getter private DailyRuns dailyRuns = new DailyRuns();
    @Getter private Treasures treasures = new Treasures();
    private transient boolean initialized;

    public @NotNull ConcurrentList<Dungeon.Class> getClasses() {
        if (!this.initialized)
            this.initialize();

        return this.classList;
    }

    public @NotNull Dungeon.Class getClass(@NotNull Dungeon.Class.Type classType) {
        return this.getClasses()
            .stream()
            .filter(dungeonClass -> dungeonClass.getType() == classType)
            .findFirst()
            .orElseThrow();
    }

    public @NotNull ConcurrentList<Dungeon> getDungeons() {
        if (!this.initialized)
            this.initialize();

        return this.dungeonList;
    }

    public @NotNull Dungeon getDungeon(@NotNull Dungeon.Type dungeonType) {
        return this.getDungeons()
            .stream()
            .filter(dungeon -> dungeon.getType() == dungeonType)
            .findFirst()
            .orElseThrow();
    }

    private void initialize() {
        // Initialize Dungeons
        this.dungeonMap.stream()
            .filter(entry -> entry.getKey().containsExperience())
            .findFirst()
            .ifPresent(entry -> {
                entry.getValue().type = entry.getKey();

                this.dungeonMap.stream()
                    .filter(other -> !entry.getKey().containsExperience())
                    .forEach(other -> {
                        other.getValue().type = other.getKey();
                        other.getValue().experience = entry.getValue().getExperience();
                    });
            });
        this.dungeonList = this.dungeonMap.stream()
            .map(Map.Entry::getValue)
            .collect(Concurrent.toList());

        // Initialize Classes
        this.classList = this.classMap.stream()
            .map(entry -> new Dungeon.Class(entry.getKey(), entry.getValue().get("experience")))
            .collect(Concurrent.toList());

        this.initialized = true;
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
            private Dungeon.Type dungeonType;
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

                public @NotNull Dungeon.Class.Type getClassType() {
                    return Dungeon.Class.Type.of(DISPLAY_PATTERN.matcher(this.getDisplayName()).group(3).toUpperCase());
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
            private @NotNull  UUID chestId;
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
