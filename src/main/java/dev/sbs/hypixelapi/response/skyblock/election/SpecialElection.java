package dev.sbs.minecraftapi.client.hypixel.response.skyblock.election;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class SpecialElection extends Election {

    private final @NotNull String specialMayor;

    public SpecialElection(int year, @NotNull String specialMayor) {
        super(year);
        this.specialMayor = specialMayor;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SpecialElection that = (SpecialElection) o;

        return new EqualsBuilder()
            .append(this.getSpecialMayor(), that.getSpecialMayor())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(this.getSpecialMayor())
            .build();
    }

    @Override
    public String toString() {
        return String.format(
            "SpecialElection{specialMayor='%s', year=%d, voting=%s, term=%s}",
            this.getSpecialMayor(),
            this.getYear(),
            this.getVoting(),
            this.getTerm()
        );
    }

}
