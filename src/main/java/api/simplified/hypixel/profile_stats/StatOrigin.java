package api.simplified.hypixel.profile_stats;

import org.jetbrains.annotations.NotNull;

/**
 * A named place one stat value came from.
 * <p>
 * Every bucket enum implements this, which is what lets one table hold a profile source and an item
 * bucket side by side rather than parameterising the table by whichever enum its owner declares.
 */
public interface StatOrigin {

    /**
     * The constant's name, as the enum declares it.
     */
    @NotNull String name();

    /**
     * Whether a value from this origin is fixed for the member, so an optimiser can total it once
     * rather than recomputing it per candidate loadout.
     */
    boolean isOptimizerConstant();

}
