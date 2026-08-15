package api.simplified.hypixel.response.resource;

import api.simplified.hypixel.response.skyblock.election.Election;
import api.simplified.hypixel.response.skyblock.election.Mayor;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;

/**
 * The Hypixel SkyBlock mayor currently in office, together with the election running to choose the
 * next one.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceElection {

    /**
     * Whether the wire reported the request as successful.
     */
    private boolean success;

    /**
     * When the resource was last regenerated.
     */
    private @NotNull Instant lastUpdated = Instant.now();

    /**
     * The mayor currently in office.
     */
    private @NotNull Mayor mayor = new Mayor();

    /**
     * The election currently taking votes, together with the ballot standing in it, absent while
     * voting is closed.
     */
    @SerializedName("current")
    private @NotNull Optional<Election> currentElection = Optional.empty();

}
