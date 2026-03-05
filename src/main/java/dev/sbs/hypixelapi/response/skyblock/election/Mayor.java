package dev.sbs.minecraftapi.client.hypixel.response.skyblock.election;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
@NoArgsConstructor
public class Mayor extends Candidate {

    private @NotNull Optional<VotingCandidate> minister = Optional.empty();
    @SerializedName("election")
    private @NotNull Election electionResults = new Election();

}
