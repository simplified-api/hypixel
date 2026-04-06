package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

@Getter
public class WyldWoods {

    @SerializedName("talked_threebrothers")
    private @NotNull ConcurrentList<String> talkedThreebrothers = Concurrent.newList();
    @SerializedName("bughunter_step")
    private int bughunterStep;
    @Accessors(fluent = true)
    @SerializedName("sirius_started_q_a")
    private boolean hasStartedSiriusQA;
    @SerializedName("sirius_q_a_chain_done")
    private boolean siriusQAChainDone;
    @Accessors(fluent = true)
    @SerializedName("sirius_completed_q_a")
    private boolean hasCompletedSiriusQA;
    @Accessors(fluent = true)
    @SerializedName("sirius_claimed_doubloon")
    private boolean hasClaimedSiriusDoubloon;

}
