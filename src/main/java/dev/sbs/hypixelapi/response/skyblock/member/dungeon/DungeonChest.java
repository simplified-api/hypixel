package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.dungeon;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.io.gson.SerializedPath;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class DungeonChest {

    @SerializedName("run_id")
    private @NotNull UUID runId;
    @SerializedName("chest_id")
    private @NotNull UUID chestId;
    @SerializedName("treasure_type")
    private @NotNull Type type;
    private int quality;
    @SerializedName("shiny_eligible")
    private boolean shinyEligible;
    private boolean paid;
    private int rerolls;
    @SerializedPath("rewards.rewards")
    private @NotNull ConcurrentList<String> items = Concurrent.newList();
    @SerializedPath("rewards.rolled_rng_meter_randomly")
    private boolean rolledRngMeterRandomly;

    public enum Type {

        WOOD,
        GOLD,
        DIAMOND,
        EMERALD,
        OBSIDIAN,
        BEDROCK

    }

}
