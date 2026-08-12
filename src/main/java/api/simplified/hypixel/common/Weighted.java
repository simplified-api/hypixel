package api.simplified.hypixel.common;

import org.jetbrains.annotations.NotNull;

/**
 * A progression that carries a weight, so it can be scored against its peers.
 * <p>
 * The formula belongs to the implementation, since a skill, a slayer and a dungeon each scale
 * differently; all this contract fixes is that one is available.
 */
public interface Weighted {

    /**
     * Weight this progression contributes, calculated on demand.
     */
    @NotNull Weight getWeight();

}
