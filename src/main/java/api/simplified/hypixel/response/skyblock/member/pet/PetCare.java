package api.simplified.hypixel.response.skyblock.member.pet;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * What a member has handed over at the Pet Care building.
 *
 * <p>
 * Bea's home houses the pet NPCs. Ten specific pets given to George each raise the Taming level cap
 * by one, from 50 to 60, and are consumed rather than handed back. The whole node is absent for a
 * member who never used the building, which reads as an empty instance rather than as null.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Pet_Care">Pet Care</a>
 */
@Getter
public class PetCare {

    /**
     * Coins spent on pet care.
     */
    @SerializedName("coins_spent")
    public double coinsSpent;

    /**
     * Pet type ids already given to George, each worth one Taming level past 50. Sacrificed is the
     * wire's word for it; in game the pet is given away and does not come back.
     */
    @SerializedName("pet_types_sacrificed")
    private @NotNull ConcurrentList<String> sacrificedPets = Concurrent.newList();

}
