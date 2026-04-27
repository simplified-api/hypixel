package api.simplified.hypixel.response.resource;

import api.simplified.hypixel.response.skyblock.election.Election;
import api.simplified.hypixel.response.skyblock.election.Mayor;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;

/**
 * Information regarding the current Mayor and ongoing Election.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceElection {

    private boolean success;
    private @NotNull Instant lastUpdated = Instant.now();
    private @NotNull Mayor mayor = new Mayor();
    @SerializedName("current")
    private @NotNull Optional<Election> currentElection = Optional.empty();

}
