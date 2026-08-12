package api.simplified.hypixel.profile_stats;

import api.simplified.skyblock.model.Stat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * What a buff key's prefix does to the running total.
 * <p>
 * The prefix decides the operation and nothing else does, so a row that wants a multiply says
 * {@code MULTIPLY_}. A value that happens to be a plain number is an operand like any other.
 */
@Getter
@RequiredArgsConstructor
public enum Operation {

    /**
     * No prefix at all - the key is a bare stat id and the operand is added.
     */
    NONE("", Pass.BONUS) {

        @Override
        public double apply(double current, double operand) {
            return current + operand;
        }

    },

    /**
     * Adds the operand to the running total.
     */
    ADD("ADD_", Pass.BONUS) {

        @Override
        public double apply(double current, double operand) {
            return current + operand;
        }

    },

    /**
     * Multiplies the running total by the operand, and is skipped for a stat that cannot be
     * multiplied.
     */
    MULTIPLY("MULTIPLY_", Pass.BONUS) {

        @Override
        public double apply(double current, double operand) {
            return current * operand;
        }

        @Override
        public boolean appliesTo(@NotNull Stat statModel) {
            return statModel.isMultiplicable();
        }

    },

    /**
     * Scales the finished total, which is why it runs only once every other source has contributed.
     */
    COPY("COPY_", Pass.POST) {

        @Override
        public double apply(double current, double operand) {
            return current * operand;
        }

    },

    /**
     * Replaces the running total with the operand.
     */
    SET("SET_", Pass.POST) {

        @Override
        public double apply(double current, double operand) {
            return operand;
        }

    };

    /**
     * The literal the buff key starts with, empty for the unprefixed form.
     */
    private final @NotNull String prefix;

    /**
     * Which pass reads this operation.
     */
    private final @NotNull Pass pass;

    /**
     * Folds the operand into the running total.
     *
     * @param current the value accumulated so far
     * @param operand the number the buff's value evaluated to
     * @return the adjusted value
     */
    public abstract double apply(double current, double operand);

    /**
     * Whether this operation may touch a given stat at all.
     *
     * @param statModel the stat being totalled
     * @return {@code true} unless the stat forbids this operation
     */
    public boolean appliesTo(@NotNull Stat statModel) {
        return true;
    }

    /**
     * Finds the operation a buff key's prefix names.
     *
     * @param buffKey the whole buff key
     * @return the operation, {@link #NONE} for an unprefixed key
     */
    public static @NotNull Operation of(@NotNull String buffKey) {
        return Arrays.stream(values())
            .filter(operation -> !operation.getPrefix().isEmpty())
            .filter(operation -> buffKey.startsWith(operation.getPrefix()))
            .findFirst()
            .orElse(NONE);
    }

    /**
     * Which pass an operation belongs to.
     */
    public enum Pass {

        /**
         * Runs against the flat totals, once every source has contributed.
         */
        BONUS,

        /**
         * Runs against the finished totals, after the bonus pass.
         */
        POST

    }

}
