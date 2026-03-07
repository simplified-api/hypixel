package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
public class StillgoreChateau {

    @Accessors(fluent = true)
    @SerializedName("unlocked_pathway_skip")
    private boolean hasUnlockedPathwaySkip;
    @SerializedName("fairy_step")
    private int fairyStep;
    @SerializedName("grubber_stacks")
    private int grubberStacks;

}
