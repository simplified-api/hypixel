package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
public class BlackLagoon {

    @Accessors(fluent = true)
    @SerializedName("talked_to_edwin")
    private boolean hasTalkedToEdwin;
    @Accessors(fluent = true)
    @SerializedName("received_science_paper")
    private boolean hasReceivedSciencePaper;
    @Accessors(fluent = true)
    @SerializedName("delivered_science_paper")
    private boolean hasDeliveredSciencePaper;
    @SerializedName("completed_step")
    private int completedStep;

}
