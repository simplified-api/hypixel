package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.date.SkyBlockDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
@NoArgsConstructor
public class PabloQuest {

    @SerializedName("pablo_item")
    private @NotNull Optional<String> item = Optional.empty();
    @SerializedName("pablo_active")
    private boolean active;
    @SerializedName("pablo_last_give")
    private @NotNull Optional<SkyBlockDate.RealTime> lastGive = Optional.empty();

}
