package api.simplified.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NamingStyle;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

/**
 * A member's progress in the Wyld Woods, the Rift's take on the Wilderness and the first area past
 * the Wizard Tower.
 *
 * <p>
 * Enigma's Crib, the Broken Cage and the way into the Black Lagoon all hang off it. Three unrelated
 * threads are tracked here - the three brothers' dialogue, the bug hunter's chain, and Sirius's
 * question-and-answer chain, which ends in a claimable doubloon.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Wyld_Woods">Wyld Woods</a>
 */
@Getter
public class WyldWoods {

    /**
     * Ids of the three brothers spoken to, bound from {@code talked_threebrothers}, whose
     * run-together spelling the field name keeps. The ids are zero-indexed - {@code threebrother_0}
     * through {@code threebrother_2} - and the wire does not order them.
     */
    @SerializedName("talked_threebrothers")
    private @NotNull ConcurrentList<String> talkedThreebrothers = Concurrent.newList();

    /**
     * How far the bug hunter's chain has run, bound from {@code bughunter_step}.
     */
    @SerializedName("bughunter_step")
    private int bughunterStep;

    /**
     * Whether Sirius's question-and-answer chain has been started, bound from
     * {@code sirius_started_q_a}.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("sirius_started_q_a")
    private boolean hasStartedSiriusQA;

    /**
     * Whether Sirius's question-and-answer chain has run to its end, bound from
     * {@code sirius_q_a_chain_done}. The wire reports this separately from the completion flag and
     * gives no ordering between the two.
     */
    @SerializedName("sirius_q_a_chain_done")
    private boolean siriusQAChainDone;

    /**
     * Whether Sirius's question-and-answer chain has been completed, bound from
     * {@code sirius_completed_q_a}.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("sirius_completed_q_a")
    private boolean hasCompletedSiriusQA;

    /**
     * Whether the doubloon Sirius's chain ends in has been claimed, bound from
     * {@code sirius_claimed_doubloon}.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("sirius_claimed_doubloon")
    private boolean hasClaimedSiriusDoubloon;

}
