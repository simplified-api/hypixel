package api.simplified.hypixel.response.skyblock.election;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * An election won by a special mayor rather than by one of the rotating candidates.
 * <p>
 * Special candidates stand every eighth SkyBlock year instead of by random draw, and arrive
 * carrying their whole perk set. Identity adds the special mayor's name to the year. There is no
 * no-arg constructor, so this is built in code rather than bound from the wire.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Mayor_Election">Mayor Election</a>
 */
@Getter
public class SpecialElection extends Election {

    /**
     * Name of the special mayor that took office for this election's term.
     */
    private final @NotNull String specialMayor;

    /**
     * Constructs an election won by a named special mayor.
     *
     * @param year the SkyBlock year the election runs in
     * @param specialMayor the name of the special mayor that won it
     */
    public SpecialElection(int year, @NotNull String specialMayor) {
        super(year);
        this.specialMayor = specialMayor;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SpecialElection that = (SpecialElection) o;

        return Objects.equals(this.getSpecialMayor(), that.getSpecialMayor());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.getSpecialMayor());
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
