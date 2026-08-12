package api.simplified.hypixel.common;

import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * The four aggregates a group of weighted progressions reports over its members.
 * <p>
 * Only the progressions the group chooses to expose are folded, which is not always every one it
 * holds - a cosmetic skill counts toward a member's levels without counting toward their weight.
 *
 * @param <T> the progression type being folded
 */
public interface WeightedGroup<T extends Experience & Weighted> {

    /**
     * The members the aggregates fold over, which is not always every member the group holds.
     */
    @NotNull ConcurrentList<T> getWeighted();

    /**
     * Mean level across the members, zero when there are none.
     */
    default double getAverage() {
        return this.getWeighted()
            .stream()
            .mapToDouble(Experience::getLevel)
            .average()
            .orElse(0.0);
    }

    /**
     * Total experience across the members.
     */
    default double getExperience() {
        return this.getWeighted()
            .stream()
            .mapToDouble(Experience::getExperience)
            .sum();
    }

    /**
     * Mean lifetime progress across the members, zero when there are none.
     */
    default double getProgressPercentage() {
        return this.getWeighted()
            .stream()
            .mapToDouble(Experience::getTotalProgressPercentage)
            .average()
            .orElse(0.0);
    }

    /**
     * Weight of each member, keyed by the member itself.
     */
    default @NotNull ConcurrentMap<T, Weight> getWeight() {
        return this.getWeighted()
            .stream()
            .map(member -> Pair.of(member, member.getWeight()))
            .collect(Concurrent.toMap());
    }

}
