package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.PairOptional;
import dev.simplified.gson.annotation.Capture;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Split;
import dev.simplified.util.Range;
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
    @Extract("completedTiers")
    private @NotNull ConcurrentMap<String, Integer> unknownTiers = Concurrent.newMap();

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
        @SerializedName("combat_level")
        @Split("-")
        private @NotNull PairOptional<Integer, Integer> combatLevel = PairOptional.empty();

        public @NotNull Range<Integer> getCombatLevel() {
            return this.combatLevel.map(Range::between).orElse(Range.between(0, 60));
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
