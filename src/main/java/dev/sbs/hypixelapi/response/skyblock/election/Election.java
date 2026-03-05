package dev.sbs.minecraftapi.client.hypixel.response.skyblock.election;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.date.Season;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.date.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor
public class Election implements PostInit {

    private int year;
    private transient Cycle voting;
    private transient Cycle term;

    public Election(int year) {
        this.year = year;
        this.postInit();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Election election = (Election) o;

        return new EqualsBuilder()
            .append(this.getYear(), election.getYear())
            .append(this.getVoting(), election.getVoting())
            .append(this.getTerm(), election.getTerm())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getYear())
            .append(this.getVoting())
            .append(this.getTerm())
            .build();
    }

    @Override
    public void postInit() {
        this.voting = new Cycle(
            new SkyBlockDate(this.getYear(), Season.LATE_SUMMER, 27, 0),
            new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0)
        );
        this.term = new Cycle(
            new SkyBlockDate(this.getYear() + 1, Season.LATE_SPRING, 27, 0),
            new SkyBlockDate(this.getYear() + 2, Season.LATE_SPRING, 27, 0)
        );
    }

    @Override
    public String toString() {
        return String.format("Election{year=%d, voting=%s, term=%s}", this.getYear(), this.getVoting(), this.getTerm());
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Cycle {

        private final @NotNull SkyBlockDate start;
        private final @NotNull SkyBlockDate end;

        @Override
        public String toString() {
            return String.format("Cycle{start=%s, end=%s}", this.getStart(), this.getEnd());
        }

    }

}
