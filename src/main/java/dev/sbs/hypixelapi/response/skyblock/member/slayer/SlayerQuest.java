package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.slayer;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Getter
public class SlayerQuest {

    private @NotNull String id = "UNKNOWN";
    private int tier;
    @SerializedName("start_timestamp")
    private Instant start;
    @SerializedName("completion_state")
    private int completionState;
    @SerializedName("used_armor")
    private boolean usedArmor;
    private boolean solo;

}