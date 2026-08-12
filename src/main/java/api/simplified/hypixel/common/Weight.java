package api.simplified.hypixel.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * A single progression's contribution to a member's score, split into capped and uncapped halves.
 * <p>
 * Weight stops climbing once a progression reaches its maximum level, so everything earned past that
 * point is counted separately as overflow. Keeping the two apart is what lets a maxed progression be
 * compared both at its cap and beyond it.
 */
public interface Weight {

    /**
     * Weight earned up to the maximum level.
     */
    double getValue();

    /**
     * Weight earned past the maximum level, zero until the progression is capped.
     */
    double getOverflow();

    /**
     * Both halves added together.
     */
    default double getTotal() {
        return this.getValue() + this.getOverflow();
    }

    /**
     * Pairs a capped weight with its overflow.
     *
     * @param value the weight earned up to the maximum level
     * @param overflow the weight earned past it
     * @return the paired weight
     */
    static @NotNull Weight of(double value, double overflow) {
        return new WeightImpl(value, overflow);
    }

    /**
     * A weight holding two values already calculated by the progression that produced them.
     */
    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PUBLIC)
    class WeightImpl implements Weight {

        /**
         * Weight earned up to the maximum level.
         */
        private final double value;

        /**
         * Weight earned past the maximum level.
         */
        private final double overflow;

    }

}
