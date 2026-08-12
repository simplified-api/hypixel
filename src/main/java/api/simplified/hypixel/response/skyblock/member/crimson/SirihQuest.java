package api.simplified.hypixel.response.skyblock.member.crimson;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Sirih's sulphur collection - the running total handed in, when the last handover was, and how far
 * through her dialogue the member has got.
 * <p>
 * The handover key here is a bare {@code last_give} where Pablo's is the prefixed
 * {@code pablo_last_give}; the same concept has two spellings and each class must use its own.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Sirih">Sirih</a>
 */
@Getter
@NoArgsConstructor
public class SirihQuest {

    /**
     * Sulphur the member has handed to Sirih so far.
     */
    @SerializedName("sulphur_given")
    private int sulphurGiven;

    /**
     * When the last handover happened.
     */
    @SerializedName("last_give")
    private @NotNull Optional<SkyBlockDate.RealTime> lastGive = Optional.empty();

    /**
     * Which line of Sirih's dialogue comes next. It advances on its own clock rather than tracking the
     * sulphur total, and nothing here says what moves it.
     */
    @SerializedName("dialogue_index")
    private int dialogueIndex;

}
