package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.date.SkyBlockDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
@NoArgsConstructor
public class SirihQuest {

    @SerializedName("sulphur_given")
    private int sulphurGiven;
    @SerializedName("last_give")
    private @NotNull Optional<SkyBlockDate.RealTime> lastGive = Optional.empty();
    @SerializedName("dialogue_index")
    private int dialogueIndex;

}
