package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.crimson_isle;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.stream.pair.Pair;
import dev.sbs.api.util.Range;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

@Getter
public class CrimsonIsle implements PostInit {

    private Dojo dojo;
    @Getter(AccessLevel.NONE)
    @SerializedName("dojo")
    private @NotNull ConcurrentMap<String, Integer> dojoMap = Concurrent.newMap();
    private Abiphone abiphone = new Abiphone();
    private Matriarch matriarch = new Matriarch();
    @SerializedName("last_minibosses_killed")
    private @NotNull ConcurrentList<String> lastMinibossesKilled = Concurrent.newList();

    // Factions
    @SerializedName("selected_faction")
    private Faction selectedFaction = Faction.NONE;
    @SerializedName("mages_reputation")
    private int mageReputation;
    @SerializedName("barbarians_reputation")
    private int barbarianReputation;

    // Kuudra
    private Kuudra kuudra;
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, Integer> kuudra_completed_tiers = Concurrent.newMap();
    @Getter(AccessLevel.NONE)
    @SerializedPath("kuudra_party_finder.search_settings")
    private Kuudra.SearchSettings kuudra_search_settings = new Kuudra.SearchSettings();
    @Getter(AccessLevel.NONE)
    @SerializedPath("kuudra_party_finder.group_builder")
    private Kuudra.GroupBuilder kuudra_group_builder = new Kuudra.GroupBuilder();

    @Override
    public void postInit() {
        this.dojo = new Dojo(this.dojoMap);
        this.kuudra = new Kuudra(
            this.kuudra_completed_tiers,
            this.kuudra_search_settings,
            this.kuudra_group_builder
        );
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Abiphone {

        @SerializedName("contact_data")
        private @NotNull ConcurrentMap<String, Contact> contacts = Concurrent.newMap();
        private @NotNull ConcurrentMap<String, Integer> games = Concurrent.newMap();
        @SerializedName("active_contacts")
        private @NotNull ConcurrentList<String> collectedContacts = Concurrent.newList();

        @SerializedPath("operator_chip.repaired_index")
        private int repairedOperatorRelays;
        @SerializedName("trio_contact_addons")
        private int trioContactAddons;
        @SerializedName("selected_ringtone")
        private String selectedRingtone;

        @Getter
        @NoArgsConstructor(access = AccessLevel.NONE)
        public static class Contact {

            @SerializedName("talked_to")
            private boolean talkedTo;
            @SerializedName("completed_quest")
            private boolean questCompleted;
            @SerializedName("incoming_calls_count")
            private int incomingCalls;
            private @NotNull ConcurrentMap<String, Object> specific = Concurrent.newMap();
            @SerializedName("last_call")
            private @NotNull Optional<SkyBlockDate.RealTime> lastCall = Optional.empty();

        }

    }

    public enum Faction {

        NONE,
        @SerializedName("mages")
        MAGE,
        @SerializedName("barbarians")
        BARBARIAN

    }

    @Getter
    public static class Matriarch {

        @SerializedName("pearls_collected")
        private int lastCollectedPearls;
        @SerializedName("last_attempt")
        private SkyBlockDate.RealTime lastAttempt;
        @SerializedName("recent_refreshes")
        private ConcurrentList<SkyBlockDate.RealTime> recentRefreshes = Concurrent.newList();

    }

    @Getter
    public static class Dojo {

        private final @NotNull ConcurrentMap<Type, Integer> points;

        private Dojo(@NotNull ConcurrentMap<String, Integer> dojo) {
            this.points = Concurrent.newUnmodifiableMap(
                dojo.stream()
                    .filter(entry -> !entry.getKey().contains("time_"))
                    .map(entry -> Pair.of(Type.of(entry.getKey().replace("dojo_points_", "")), entry.getValue()))
                    .collect(Concurrent.toMap())
            );
        }

        public int getPoints(@NotNull Type type) {
            return this.getPoints().getOrDefault(type, 0);
        }

        @Getter
        @RequiredArgsConstructor
        public enum Type {

            UNKNOWN(""),
            FORCE("mob_kb"),
            STAMINA("wall_jump"),
            MASTERY("archer"),
            DISCIPLINE("sword_swap"),
            SWIFTNESS("snake"),
            CONTROL("fireball"),
            TENACITY("lock_head");

            private final @NotNull String internalName;

            public static @NotNull Type of(@NotNull String name) {
                return Arrays.stream(values())
                    .filter(type -> type.name().equalsIgnoreCase(name) || type.getInternalName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(UNKNOWN);
            }

        }

    }

    @Getter
    public static class Kuudra {

        private final @NotNull ConcurrentMap<Tier, Integer> completedTiers;
        private final @NotNull ConcurrentMap<Tier, Integer> highestWave;
        private final @NotNull SearchSettings searchSettings;
        private final @NotNull GroupBuilder groupBuilder;

        private Kuudra(@NotNull ConcurrentMap<String, Integer> kuudraCompletedTiers, @Nullable Kuudra.SearchSettings kuudraSearchSettings, @Nullable Kuudra.GroupBuilder kuudraGroupBuilder) {
            this.searchSettings = (kuudraSearchSettings != null ? kuudraSearchSettings : new SearchSettings());
            this.groupBuilder = (kuudraGroupBuilder != null ? kuudraGroupBuilder : new GroupBuilder());

            this.completedTiers = kuudraCompletedTiers.stream()
                .filter(entry -> !entry.getKey().startsWith("highest_"))
                .map(entry -> Pair.of(Tier.of(entry.getKey()), entry.getValue()))
                .collect(Concurrent.toUnmodifiableMap());

            this.highestWave = Concurrent.newUnmodifiableMap(
                kuudraCompletedTiers.stream()
                    .filter(entry -> entry.getKey().startsWith("highest_"))
                    .map(entry -> Pair.of(Tier.of(entry.getKey()), entry.getValue()))
                    .collect(Concurrent.toUnmodifiableMap())
            );
        }

        @Getter
        @RequiredArgsConstructor
        public enum Tier {

            UNKNOWN,
            BASIC("NONE"),
            HOT,
            BURNING,
            FIERY,
            INFERNAL;

            private final @NotNull String internalName;

            Tier() {
                this.internalName = name();
            }

            public @NotNull String getName() {
                return StringUtil.capitalizeFully(this.name());
            }

            public static @NotNull Tier of(String name) {
                return Arrays.stream(values())
                    .filter(tier -> tier.name().equalsIgnoreCase(name) || tier.getInternalName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(UNKNOWN);
            }

        }

        @Getter
        public static class SearchSettings {

            private Tier tier = Tier.BASIC;
            private Optional<String> search = Optional.empty();
            private Sort sort = Sort.RECENTLY_CREATED;
            @Getter(AccessLevel.NONE)
            private Optional<String> combat_level = Optional.empty();

            public @NotNull Range<Integer> getCombatLevel() {
                return this.combat_level.map(range -> StringUtil.split(range, "-"))
                    .map(parts -> Range.between(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])))
                    .orElse(Range.between(0, 60));
            }

            public enum Sort {

                RECENTLY_CREATED,
                HIGHEST_COMBAT_LEVEL,
                LARGEST_GROUP_SIZE

            }

        }

        @Getter
        public static class GroupBuilder {

            private Tier tier = Tier.BASIC;
            private Optional<String> note = Optional.empty();
            @SerializedName("combat_level_required")
            private int requiredCombatLevel;

        }

    }

}
