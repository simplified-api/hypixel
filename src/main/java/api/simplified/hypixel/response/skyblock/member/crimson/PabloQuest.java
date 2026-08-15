package api.simplified.hypixel.response.skyblock.member.crimson;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Pablo's flower request - the item he is currently asking for, whether the request is live, and when
 * the member last handed one over.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Pablo">Pablo</a>
 */
@Getter
@NoArgsConstructor
public class PabloQuest {

    /**
     * The item id Pablo currently wants, a bare wire id that reaches no repository. It stays populated
     * after a request closes, so a value here does not mean the request is open.
     */
    @SerializedName("pablo_item")
    private @NotNull Optional<String> item = Optional.empty();

    /**
     * Whether the request is currently open, which is the only member here that says so.
     */
    @SerializedName("pablo_active")
    private boolean active;

    /**
     * When the member last gave Pablo his item.
     */
    @SerializedName("pablo_last_give")
    private @NotNull Optional<SkyBlockDate.RealTime> lastGive = Optional.empty();

}
