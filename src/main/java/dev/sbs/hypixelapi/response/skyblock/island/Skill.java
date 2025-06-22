package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public class Skill {

    protected final @NotNull Type type;
    protected final double experience;

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