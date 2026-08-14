package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.skyblock.model.Stat;
import org.jetbrains.annotations.NotNull;

/**
 * The write face of a stat sheet, handed to a source so it can contribute without reading back.
 * <p>
 * A source that cannot read a total cannot depend on another source having run, so the flat pass has
 * no order to get wrong. Totals are published between passes instead, where every source has
 * finished and the number is settled.
 * <p>
 * Two of the three calls write at the profile level and the third answers somewhere else to write.
 * None of them answers a number.
 */
interface StatSink {

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

    /**
     * Files an item instance under a slot and answers where that slot's cells go.
     * <p>
     * What comes back is the write face of that slot's table rather than the table, so a source
     * filling a slot can no more read it than it can read the profile.
     *
     * @param slot the address to file the instance under
     * @param stack the instance filling it
     * @return the write face of that slot's table
     */
    @NotNull StatSink open(@NotNull ItemSlot slot, @NotNull ItemStack stack);

}
