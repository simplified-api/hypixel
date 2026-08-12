package api.simplified.hypixel.response.skyblock.member.pet;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.StreamUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Every pet a member owns, together with the two side systems that hang off the pet menu.
 *
 * <p>
 * A pet is a summonable companion granting stats and perks scaled by its level and rarity, and at
 * most one is summoned at a time. Bound from the wire's {@code pets_data} node.
 *
 * <p>
 * Every accessor here is derived and recomputes on each call - nothing on this class memoises.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Pets">Pets</a>
 */
@Getter
public class Pets {

    /**
     * The thirteen pet score milestones, ascending, each of which grants the member a further point
     * of magic find once the score reaches it.
     */
    private final static @NotNull ConcurrentList<Integer> magicFindPetScore = Concurrent.newList(10, 25, 50, 75, 100, 130, 175, 225, 275, 325, 375, 450, 500);

    /**
     * Every pet the member owns, summoned or not.
     */
    private @NotNull ConcurrentList<OwnedPet> pets = Concurrent.newList();

    /**
     * What the member has handed over at the Pet Care building, empty rather than null when the wire
     * omits the node.
     */
    @SerializedName("pet_care")
    private @NotNull PetCare petCare = new PetCare();

    /**
     * The member's autopet rules.
     */
    private @NotNull AutoPet autopet = new AutoPet();

    /**
     * The pet currently summoned, empty when none is out. Derived by scanning the owned pets on
     * every call.
     */
    public @NotNull Optional<OwnedPet> getActivePet() {
        return this.getPets()
            .stream()
            .filter(OwnedPet::isActive)
            .findFirst();
    }

    /**
     * The member's pet score, summing the score of each distinct pet id.
     * <p>
     * The owned pets are sorted by rarity descending before duplicates are dropped, so a pet the
     * member holds twice contributes its highest-rarity copy, which is the in-game rule. The extra
     * point the game awards a pet sitting at its level cap is not counted, so the figure here is a
     * floor rather than the number shown in game.
     */
    public int getPetScore() {
        return this.getPets()
            .sorted(OwnedPet::getRarity)
            .stream()
            .filter(StreamUtil.distinctByKey(OwnedPet::getId))
            .mapToInt(OwnedPet::getScore)
            .sum();
    }

    /**
     * The highest pet score milestone the member's pet score has reached, or zero below the first one.
     */
    public int getMilestone() {
        return magicFindPetScore.matchAll(breakpoint -> breakpoint <= this.getPetScore())
            .reduce((m1, m2) -> m2)
            .orElse(0);
    }

}
