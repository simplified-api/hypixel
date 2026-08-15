package api.simplified.hypixel.common;

import dev.simplified.annotations.UtilityClass;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.util.NumberUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Groups the flat {@code <id>_<n>} lists Hypixel sends wherever an id carries a number.
 */
@UtilityClass
public final class IdTiers {

    /**
     * Splits each entry at its last underscore and groups the numbers under the id.
     * <p>
     * The split is at the last underscore because an id may carry its own - {@code METAL_HEART_2} is
     * tier 2 of {@code METAL_HEART}, and {@code LOG_2_5} is tier 5 of {@code LOG_2}. Entries with no
     * underscore, or whose suffix is not a number, are skipped. Negative numbers survive, since only
     * the caller knows whether they mean anything.
     *
     * @param entries the flat wire list
     * @return the numbers found for each id, in the order the wire gave them
     */
    public static @NotNull ConcurrentMap<String, ConcurrentList<Integer>> group(@NotNull ConcurrentList<String> entries) {
        ConcurrentMap<String, ConcurrentList<Integer>> grouped = Concurrent.newMap();

        for (String entry : entries) {
            int split = entry.lastIndexOf('_');
            if (split < 0) continue;

            Integer tier = NumberUtil.tryParseInt(entry.substring(split + 1));
            if (tier == null) continue;

            grouped.computeIfAbsent(entry.substring(0, split), id -> Concurrent.newList()).add(tier);
        }

        return grouped;
    }

}
