package api.simplified.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;

/**
 * A member's progress in the Rift's Wizard Tower, where the portal from the Hub arrives.
 *
 * <p>
 * Rift Time does not tick down above the tower's bottom floor, so it doubles as the dimension's safe
 * room, and the Wizard at the top is who unlocks the Rift Guide. This is the Rift's tower, not the
 * Hub sub-location of the same name.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Wizard_Tower_(Rift)">Wizard Tower (Rift)</a>
 */
@Getter
public class WizardTower {

    /**
     * How far the Wizard's dialogue chain has run, bound from {@code wizard_quest_step}.
     */
    @SerializedName("wizard_quest_step")
    private int wizardQuestStep;

    /**
     * Wizard's Breadcrumbs placed to mark a path back to the tower, bound from
     * {@code crumbs_laid_out}.
     */
    @SerializedName("crumbs_laid_out")
    private int crumbsLaidOut;

}
