package api.simplified.hypixel.response.skyblock.stats;

import org.jetbrains.annotations.NotNull;

/**
 * The write face of the variables a bonus expression can name.
 * <p>
 * A provider writes and never reads, which is what stops one naming a value another provider has not
 * published yet. It is the same shape the stat sink is, for the same reason.
 */
@FunctionalInterface
interface VariableSink {

    /**
     * Publishes one variable under a given name.
     *
     * @param name the name an expression refers to it by
     * @param value the value to publish
     */
    void put(@NotNull String name, double value);

}
