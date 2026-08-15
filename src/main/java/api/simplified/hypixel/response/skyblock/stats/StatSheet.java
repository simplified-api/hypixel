package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.skyblock.model.Stat;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.RequiredArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentLinkedMap;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * Everything one compute writes: the profile's own table, and one table per filled item slot.
 * <p>
 * The sheet is what a source is handed, and it is handed as a {@link StatSink} - so a source may
 * write at the profile level, may open a slot and write there, and can read neither back. Armour was
 * never blocked by needing to read anything; it was blocked by needing to write somewhere other than
 * the profile table, and {@link #open} is the whole of that.
 * <p>
 * A slot is filed the first time it is opened and holds the instance that filled it, so what a table
 * belongs to travels with the table rather than with the list a caller happened to iterate.
 */
final class StatSheet implements StatSink {

    private final @NotNull StatTable profile = new StatTable();

    private final @NotNull ConcurrentLinkedMap<ItemSlot, Slot> slots = Concurrent.newLinkedMap();

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull StatSink add(@NotNull StatOrigin origin, @NotNull String statId, @NotNull StatHalf half, double value) {
        this.profile.add(origin, statId, half, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull StatSink add(@NotNull StatOrigin origin, @NotNull Stat statModel, @NotNull StatHalf half, double value) {
        this.profile.add(origin, statModel, half, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull StatSink open(@NotNull ItemSlot slot, @NotNull ItemStack stack) {
        return new SlotSink(this.slots.computeIfAbsent(slot, key -> new Slot(stack, new StatTable())).table());
    }

    /**
     * The table the profile's own sources write into.
     */
    @NotNull StatTable getProfile() {
        return this.profile;
    }

    /**
     * Every slot something opened, in the order they were opened.
     */
    @NotNull ConcurrentLinkedMap<ItemSlot, Slot> getSlots() {
        return this.slots;
    }

    /**
     * Every slot of one family, in the order they were opened - which is the order of their indices,
     * because the constant that opens a family opens it whole.
     *
     * @param kind the family to read
     * @return what fills each slot of that family
     */
    @NotNull ConcurrentList<Slot> getSlots(@NotNull ItemSlot.Kind kind) {
        ConcurrentList<Slot> matching = Concurrent.newList();

        this.slots.forEach((address, slot) -> {
            if (address.kind() == kind)
                matching.add(slot);
        });

        return matching;
    }

    /**
     * What fills one slot.
     *
     * @param stack the instance filed there
     * @param table the cells written under it
     */
    record Slot(@NotNull ItemStack stack, @NotNull StatTable table) {

    }

    /**
     * The write face of one slot's table.
     * <p>
     * Opening from here opens on the sheet that answered it, so a slot is reached by its address
     * rather than by whichever face a source happens to be holding.
     */
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private final class SlotSink implements StatSink {

        private final @NotNull StatTable table;

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull StatSink add(@NotNull StatOrigin origin, @NotNull String statId, @NotNull StatHalf half, double value) {
            this.table.add(origin, statId, half, value);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull StatSink add(@NotNull StatOrigin origin, @NotNull Stat statModel, @NotNull StatHalf half, double value) {
            this.table.add(origin, statModel, half, value);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public @NotNull StatSink open(@NotNull ItemSlot slot, @NotNull ItemStack stack) {
            return StatSheet.this.open(slot, stack);
        }

    }

}
