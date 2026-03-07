package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.math.Range;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.api.util.StringUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

@Getter
public class Kuudra {

    private final @NotNull ConcurrentMap<Tier, Integer> completedTiers;
    private final @NotNull ConcurrentMap<Kuudra.Tier, Integer> highestWave;
    private final @NotNull Kuudra.SearchSettings searchSettings;
    private final @NotNull Kuudra.GroupBuilder groupBuilder;

    public Kuudra() {
        this(Concurrent.newMap(), null, null);
    }

    public Kuudra(@NotNull ConcurrentMap<String, Integer> kuudraCompletedTiers, @Nullable Kuudra.SearchSettings kuudraSearchSettings, @Nullable Kuudra.GroupBuilder kuudraGroupBuilder) {
        this.searchSettings = (kuudraSearchSettings != null ? kuudraSearchSettings : new Kuudra.SearchSettings());
        this.groupBuilder = (kuudraGroupBuilder != null ? kuudraGroupBuilder : new Kuudra.GroupBuilder());

        this.completedTiers = kuudraCompletedTiers.stream()
            .filter(entry -> !entry.getKey().startsWith("highest_"))
            .map(entry -> Pair.of(Kuudra.Tier.of(entry.getKey()), entry.getValue()))
            .collect(Concurrent.toUnmodifiableMap());

        this.highestWave = Concurrent.newUnmodifiableMap(
            kuudraCompletedTiers.stream()
                .filter(entry -> entry.getKey().startsWith("highest_"))
                .map(entry -> Pair.of(Kuudra.Tier.of(entry.getKey()), entry.getValue()))
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

        public static @NotNull Kuudra.Tier of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(tier -> tier.name().equalsIgnoreCase(name) || tier.getInternalName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No tier with name " + name));
        }

    }

    @Getter
    public static class SearchSettings {

        private @NotNull Kuudra.Tier tier = Kuudra.Tier.BASIC;
        private @NotNull Optional<String> search = Optional.empty();
        private @NotNull Kuudra.SearchSettings.Sort sort = Kuudra.SearchSettings.Sort.RECENTLY_CREATED;
        @Getter(AccessLevel.NONE)
        private @NotNull Optional<String> combat_level = Optional.empty();

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

        private Kuudra.Tier tier = Kuudra.Tier.BASIC;
        private Optional<String> note = Optional.empty();
        @SerializedName("combat_level_required")
        private int requiredCombatLevel;

    }

}