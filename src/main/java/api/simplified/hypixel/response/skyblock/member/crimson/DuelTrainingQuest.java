package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.date.SkyBlockDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
@NoArgsConstructor
public class DuelTrainingQuest {

    @SerializedName("duel_training_phase_barbarians")
    private int barbarianPhase;
    @SerializedName("duel_training_last_complete_barbarians")
    private @NotNull Optional<SkyBlockDate.RealTime> lastCompleteBarbarianPhase = Optional.empty();
    @SerializedName("duel_training_phase_mages")
    private int magePhase;
    @SerializedName("duel_training_last_complete_mages")
    private @NotNull Optional<SkyBlockDate.RealTime> lastCompleteMagePhase = Optional.empty();

}
