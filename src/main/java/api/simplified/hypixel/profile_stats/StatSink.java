package api.simplified.hypixel.profile_stats;

import api.simplified.hypixel.profile_stats.data.StatHalf;
import api.simplified.skyblock.model.Stat;
import org.jetbrains.annotations.NotNull;

/**
 * The write face of a stat table, handed to a source so it can contribute without reading back.
 * <p>
 * A source that cannot read a total cannot depend on another source having run, so the flat pass has
 * no order to get wrong. Totals are published between passes instead, where every source has
 * finished and the number is settled.
 */
@FunctionalInterface
public interface StatSink {

    /**
     * Adds one value to the stat of a given id.
     *
     * @param origin where the value came from
     * @param statId the id of the stat the value is for
     * @param half which half of the value to write
     * @param value the amount to add
     * @return this sink
     */
    @NotNull StatSink add(@NotNull StatOrigin origin, @NotNull String statId, @NotNull StatHalf half, double value);

    /**
     * Adds one value to a stat already in hand.
     *
     * @param origin where the value came from
     * @param statModel the stat the value is for
     * @param half which half of the value to write
     * @param value the amount to add
     * @return this sink
     */
    default @NotNull StatSink add(@NotNull StatOrigin origin, @NotNull Stat statModel, @NotNull StatHalf half, double value) {
        return this.add(origin, statModel.getId(), half, value);
    }

}
