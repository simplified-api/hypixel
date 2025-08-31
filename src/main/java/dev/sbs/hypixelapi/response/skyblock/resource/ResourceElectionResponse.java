package dev.sbs.minecraftapi.client.hypixel.response.skyblock.resource;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.election.Election;
import dev.sbs.minecraftapi.skyblock.election.Mayor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceElectionResponse {

    private boolean success;
    private @NotNull Instant lastUpdated = Instant.now();
    private @NotNull Mayor mayor = new Mayor();
    @SerializedName("current")
    private @NotNull Optional<Election> currentElection = Optional.empty();

}
