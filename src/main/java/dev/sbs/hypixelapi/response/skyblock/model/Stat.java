package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.persistence.Model;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

public interface Stat extends Model {

    double MAGIC_CONSTANT = 719.28;

    @NotNull String getId();

    @NotNull String getName();

    @NotNull String getSymbol();

    @NotNull ChatFormat getFormat();

    @NotNull String getCategory();

    double getBase();

    double getCap();

    double getEnrichment();

    double getPowerMultiplier();

    default double getPowerCoefficient() {
        return (this.getPowerMultiplier() * MAGIC_CONSTANT) / 100.0;
    }

    double getTuningMultiplier();

    boolean isVisible();

    default boolean notVisible() {
        return !this.isVisible();
    }

    @Getter
    @RequiredArgsConstructor
    enum Type {

        NONE("", ""),
        FLAT("+", ""),
        MULTIPLY("", "x"),
        PERCENT("", "%"),
        PLUS_MULTIPLY("+", "x"),
        PLUS_PERCENT("+", "%"),
        SECONDS("", "s");

        private final @NotNull String prefix;
        private final @NotNull String suffix;

        public @NotNull String format(int level, @NotNull Pet.Substitute.Value value) {
            return String.format(
                "%s%s%s",
                this.getPrefix(),
                value.getBase() + (level * value.getScalar()),
                this.getSuffix()
            );
        }

    }

}
