package dev.sbs.minecraftapi.client.hypixel.response.skyblock.election;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class VotingCandidate extends Candidate {

    private int votes;

}
