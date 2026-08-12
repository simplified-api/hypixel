package api.simplified.hypixel.response.skyblock.member.slayer;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * The slayer quest a member currently has open - which branch, which tier, when it started, and
 * whether it is being run alone.
 *
 * <p>
 * Every member is a plain bound field; nothing here derives and nothing reaches a repository. The
 * same class binds both the quest hanging off the member's slayer node and the Rift's own.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Slayer">Slayer</a>
 */
@Getter
public class SlayerQuest {

    /**
     * Branch the quest was taken against, lowercase and matching a {@link SlayerBoss} key. Falls
     * back to the literal {@code UNKNOWN} when the wire names none.
     */
    @SerializedName("type")
    private @NotNull String id = "UNKNOWN";

    /**
     * Tier of the boss the quest is for.
     */
    private int tier;

    /**
     * When the quest was accepted. There is no default, so a quest object with no start timestamp
     * leaves this null.
     */
    @SerializedName("start_timestamp")
    private Instant start;

    /**
     * Opaque state code the wire reports for the quest.
     */
    @SerializedName("completion_state")
    private int completionState;

    /**
     * Whether slayer armour bonuses were active on the quest.
     */
    @SerializedName("used_armor")
    private boolean usedArmor;

    /**
     * Whether the member is the only one on the quest.
     */
    private boolean solo;

}
