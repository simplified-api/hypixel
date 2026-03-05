package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.persistence.Model;
import org.jetbrains.annotations.NotNull;

public interface MelodySong extends Model {

    @NotNull String getId();

    @NotNull String getName();

    @NotNull Difficulty getDifficulty();

    int getIntelligenceReward();

    enum Difficulty {

        EASY,
        HARD,
        EXPERT,
        VIRTUOSO

    }

}
