package api.simplified.hypixel.response.skyblock.member.rift;

import api.simplified.hypixel.response.skyblock.member.pet.OwnedPet;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The dead cat hunt Sad Jacquelle hands out in the Wyld Woods, and the pet it unlocks.
 *
 * <p>
 * Jacquelle gives out a Cat Detector and asks for nine dead cats scattered through the Rift.
 * Finishing the hunt grants the Montezuma pet, which is then upgraded by finding Montezuma Soul
 * Pieces around the dimension.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Montezuma_Pet">Montezuma Pet</a>
 */
@Getter
public class DeadCats {

    /**
     * Whether the hunt has been accepted from Sad Jacquelle, bound from
     * {@code talked_to_jacquelle}.
     */
    @Accessors(fluent = true)
    @SerializedName("talked_to_jacquelle")
    private boolean hasTalkedToJacquelle;

    /**
     * Whether the Cat Detector has been taken, bound from {@code picked_up_detector}.
     */
    @SerializedName("picked_up_detector")
    private boolean detectorPickedUp;

    /**
     * Ids of the cats found, bound from {@code found_cats}. The ids are English ordinals and the
     * eighth of them is spelled {@code eight} on the wire, which nothing here corrects - a lookup
     * written against {@code eighth} matches nothing. The wire does not order the array either, so
     * membership means found and position means nothing.
     */
    @SerializedName("found_cats")
    private @NotNull ConcurrentList<String> foundCats = Concurrent.newList();

    /**
     * Whether the Montezuma pet has been granted, bound from {@code unlocked_pet}.
     */
    @SerializedName("unlocked_pet")
    private boolean petUnlocked;

    /**
     * The Montezuma pet itself as an ordinary {@link OwnedPet}, empty until the hunt grants it. Its
     * wire id names the Fractured Montezuma Soul rather than the pet.
     */
    private @NotNull Optional<OwnedPet> montezuma = Optional.empty();

}
