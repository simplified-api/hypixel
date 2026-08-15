package api.simplified.hypixel.response.skyblock;

import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * The Garden a single profile lookup returned.
 */
@Getter
@NoArgsConstructor
public class SkyBlockGardenResponse {

    /**
     * Whether the request was served.
     */
    private boolean success;

    /**
     * The profile's Garden, an empty one rather than null when the profile never unlocked it.
     */
    private @NotNull SkyBlockGarden garden = new SkyBlockGarden();

}
