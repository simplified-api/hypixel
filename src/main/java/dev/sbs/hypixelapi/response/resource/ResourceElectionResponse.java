package dev.sbs.minecraftapi.client.hypixel.response.resource;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Getter
public class ResourceElectionResponse {

    private boolean success;
    private Instant lastUpdated;
    private Mayor mayor;

    public Election getElection() {
        return this.getMayor().election;
    }

    @Getter
    public static class Mayor extends CandidateData {

        private Election election;

    }

    @Getter
    public static class Candidate extends CandidateData {

        private int votes;

    }

    @Getter
    private static class CandidateData {

        private String key;
        private String name;
        private @NotNull ConcurrentList<Perk> perks = Concurrent.newList();

    }

    @Getter
    public static class Perk {

        private String name;
        private String description;

    }

    @Getter
    public static class Election {

        private int year;
        private @NotNull ConcurrentList<Candidate> candidates = Concurrent.newList();

    }

}
