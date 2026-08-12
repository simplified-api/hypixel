package api.simplified.hypixel.response.skyblock.member.crimson;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A member's progress through the duel training track, which runs separately for each faction.
 * <p>
 * The two sides advance independently and switching faction resets neither, so a member sworn to one
 * can still hold a phase - and a newer clear - on the other. The wire spells each faction plural in
 * its keys while the Java names stay singular.
 */
@Getter
@NoArgsConstructor
public class DuelTrainingQuest {

    /**
     * How far through the Barbarian duel track the member is.
     */
    @SerializedName("duel_training_phase_barbarians")
    private int barbarianPhase;

    /**
     * When the member last cleared a Barbarian phase.
     */
    @SerializedName("duel_training_last_complete_barbarians")
    private @NotNull Optional<SkyBlockDate.RealTime> lastCompleteBarbarianPhase = Optional.empty();

    /**
     * How far through the Mage duel track the member is.
     */
    @SerializedName("duel_training_phase_mages")
    private int magePhase;

    /**
     * When the member last cleared a Mage phase.
     */
    @SerializedName("duel_training_last_complete_mages")
    private @NotNull Optional<SkyBlockDate.RealTime> lastCompleteMagePhase = Optional.empty();

}
