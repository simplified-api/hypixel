package api.simplified.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NamingStyle;

/**
 * A member's progress in the Stillgore Château, the Rift's castle and the home of the vampire
 * slayer.
 *
 * <p>
 * Reaching it means crossing the Photon Pathway and talking to Deer; losing every heart inside drops
 * a member into the Oubliette until its guards are killed. The wire key on the member is
 * {@code castle}, so a capture will not spell this node the way the class does.
 *
 * <p>
 * Vampire slayer progress is not here - the quest is a separate field on {@link Rift}, and so are
 * the château's counters.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Stillgore_Château">Stillgore Château</a>
 */
@Getter
public class StillgoreChateau {

    /**
     * Whether the Photon Pathway can be skipped on later visits, bound from
     * {@code unlocked_pathway_skip}.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("unlocked_pathway_skip")
    private boolean hasUnlockedPathwaySkip;

    /**
     * A step index in the château, bound from {@code fairy_step}.
     */
    @SerializedName("fairy_step")
    private int fairyStep;

    /**
     * Motes Grubber bonus stacks held, bound from {@code grubber_stacks}. Eating McGrubber's
     * Burgers raises the motes the Motes Grubber offers by 5% a stack, up to 25%.
     */
    @SerializedName("grubber_stacks")
    private int grubberStacks;

}
