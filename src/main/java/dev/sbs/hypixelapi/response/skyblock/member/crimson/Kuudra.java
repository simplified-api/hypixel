package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.Capture;
import dev.simplified.util.Range;
import dev.simplified.util.StringUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class Kuudra {

    @Capture(filter = "^highest_wave_")
    private @NotNull ConcurrentMap<Tier, Integer> highestWave = Concurrent.newMap();
    @Capture
    private @NotNull ConcurrentMap<Tier, Integer> completedTiers = Concurrent.newMap();
    transient @NotNull SearchSettings searchSettings = new SearchSettings();
    transient @NotNull GroupBuilder groupBuilder = new GroupBuilder();

    @Getter
    public enum Tier {

        @SerializedName("NONE")
        BASIC,
        HOT,
        BURNING,
        FIERY,
        INFERNAL

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
