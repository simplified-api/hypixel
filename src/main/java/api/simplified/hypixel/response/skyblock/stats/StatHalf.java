package api.simplified.hypixel.response.skyblock.stats;

import org.jetbrains.annotations.NotNull;

/**
 * Which half of a {@link Data} a value is written to.
 * <p>
 * The constant carries the access and the caller names the constant, so a rescale that has to touch
 * both halves is one loop rather than a pair of near-identical statements.
 */
public enum StatHalf {

    /**
     * The half a source provides on its own, before anything modifies it.
     */
    BASE {

        @Override
        public double read(@NotNull Data data) {
            return data.getBase();
        }

        @Override
        public void add(@NotNull Data data, double value) {
            data.base += value;
        }

        @Override
        public void set(@NotNull Data data, double value) {
            data.base = value;
        }

    },

    /**
     * The half reforges, gemstones, enrichments and item bonuses add on top.
     */
    BONUS {

        @Override
        public double read(@NotNull Data data) {
            return data.getBonus();
        }

        @Override
        public void add(@NotNull Data data, double value) {
            data.bonus += value;
        }

        @Override
        public void set(@NotNull Data data, double value) {
            data.bonus = value;
        }

    };

    /**
     * Reads this half.
     *
     * @param data the value to read from
     * @return this half's number
     */
    public abstract double read(@NotNull Data data);

    /**
     * Adds to this half.
     *
     * @param data the value to write to
     * @param value the amount to add
     */
    public abstract void add(@NotNull Data data, double value);

    /**
     * Replaces this half.
     *
     * @param data the value to write to
     * @param value the amount to write
     */
    public abstract void set(@NotNull Data data, double value);

}
