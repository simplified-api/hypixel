package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.skill;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.SimplifiedApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.JacobsContest;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.Experience;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.weight.Weight;
import dev.sbs.minecraftapi.data.model.skills.SkillModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public class Skill {

    protected final @NotNull Type type;
    protected final double experience;

    /**
     * Wraps this class in a {@link Experience} and {@link Weight} class.
     * <br><br>
     * Requires an active database session.
     */
    public @NotNull EnhancedSkill asEnhanced(@NotNull JacobsContest jacobsContest) {
        return new EnhancedSkill(this, jacobsContest);
    }

    @Getter
    @RequiredArgsConstructor
    public enum Type {

        UNKNOWN(true),
        @SerializedName("SKILL_FARMING")
        FARMING(false),
        @SerializedName("SKILL_MINING")
        MINING(false),
        @SerializedName("SKILL_COMBAT")
        COMBAT(false),
        @SerializedName("SKILL_FORAGING")
        FORAGING(false),
        @SerializedName("SKILL_FISHING")
        FISHING(false),
        @SerializedName("SKILL_ENCHANTING")
        ENCHANTING(false),
        @SerializedName("SKILL_ALCHEMY")
        ALCHEMY(false),
        @SerializedName("SKILL_CARPENTRY")
        CARPENTRY(false),
        @SerializedName("SKILL_RUNECRAFTING")
        RUNECRAFTING(true),
        @SerializedName("SKILL_SOCIAL")
        SOCIAL(true),
        @SerializedName("SKILL_TAMING")
        TAMING(false);

        private final boolean cosmetic;

        /**
         * Gets the {@link SkillModel} for the given {@link Skill}.
         * <br><br>
         * Requires an active database session.
         */
        public @NotNull SkillModel getModel() {
            if (this == UNKNOWN)
                throw new UnsupportedOperationException("Unknown does not exist in the database!");

            return SimplifiedApi.getRepositoryOf(SkillModel.class).findFirstOrNull(SkillModel::getKey, this.name());
        }

        public boolean notCosmetic() {
            return !this.isCosmetic();
        }

        public static @NotNull Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

}