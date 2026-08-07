package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.date.SkyBlockDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
@NoArgsConstructor
public class BoardQuest {

    private @NotNull Status status = Status.UNKNOWN;
    private int progress;
    @SerializedName("completed_at")
    private @NotNull Optional<SkyBlockDate.RealTime> completedAt = Optional.empty();

    public enum Status {

        UNKNOWN,
        ACTIVE,
        COMPLETED

    }

}
