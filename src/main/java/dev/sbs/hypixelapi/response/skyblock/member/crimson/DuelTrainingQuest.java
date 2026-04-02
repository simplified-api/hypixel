package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DuelTrainingQuest {

    @SerializedName("duel_training_phase_barbarians")
    private int barbarianPhase;
    @SerializedName("duel_training_last_complete_barbarians")
    private long lastCompleteBarbarianPhase;
    @SerializedName("duel_training_phase_mages")
    private int magePhase;
    @SerializedName("duel_training_last_complete_mages")
    private long lastCompleteMagePhase;

}
