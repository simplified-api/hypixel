package dev.sbs.minecraftapi.client.hypixel.response.skyblock.election;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class VotingBooth extends Election {

    private @NotNull ConcurrentList<VotingCandidate> candidates = Concurrent.newList();

}
