package api.simplified.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NamingStyle;

/**
 * A member's progress in the Black Lagoon, entered from the Wyld Woods for 4:00 of Rift Time.
 *
 * <p>
 * The one questline tracked here is a courier chain run for Edwin - the member talks to him,
 * receives a science paper and delivers it - with a step counter running alongside the three flags.
 * The lagoon's own counters are member statistics rather than location state, so none of them are
 * here.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Black_Lagoon">Black Lagoon</a>
 */
@Getter
public class BlackLagoon {

    /**
     * Whether Edwin has been spoken to, bound from {@code talked_to_edwin}.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("talked_to_edwin")
    private boolean hasTalkedToEdwin;

    /**
     * Whether the science paper has been received, bound from {@code received_science_paper}.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("received_science_paper")
    private boolean hasReceivedSciencePaper;

    /**
     * Whether the science paper has been delivered, bound from {@code delivered_science_paper}.
     */
    @Getter(style = NamingStyle.FLUENT)
    @SerializedName("delivered_science_paper")
    private boolean hasDeliveredSciencePaper;

    /**
     * How far the courier chain has run, bound from {@code completed_step}. It is reported
     * alongside the three flags rather than being derivable from them.
     */
    @SerializedName("completed_step")
    private int completedStep;

}
