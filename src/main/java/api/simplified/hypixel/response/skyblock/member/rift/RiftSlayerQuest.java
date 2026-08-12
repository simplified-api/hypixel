package api.simplified.hypixel.response.skyblock.member.rift;

import api.simplified.hypixel.response.skyblock.member.slayer.SlayerQuest;
import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * The Rift's own slayer branch, where progress is banked as combat experience against recent kills
 * rather than counted in spawns.
 *
 * <p>
 * {@link SlayerQuest} carries what every branch reports - the branch id, the tier, when the quest
 * started, whether it was solo - and this class adds the three values only the vampire branch sends.
 * The boss is the Riftstalker Bloodfiend, fought at the Stillgore Château and gated behind Sven
 * Packmaster III.
 *
 * <p>
 * The branch id arrives on the wire key {@code type}, binds to the inherited id field lowercase, and
 * is not run through an enum; an absent quest reads as the inherited default rather than as null.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Vampire_Slayer">Vampire Slayer</a>
 */
@Getter
public class RiftSlayerQuest extends SlayerQuest {

    /**
     * Combat experience banked toward the current spawn, bound from {@code combat_xp}.
     */
    @SerializedName("combat_xp")
    private int combatXP;

    /**
     * The recent kills that fed the current spawn, bound from {@code recent_mob_kills}.
     */
    @SerializedName("recent_mob_kills")
    private @NotNull ConcurrentList<MobKill> recentMobKills = Concurrent.newList();

    /**
     * The island the last counted kill happened on, bound from {@code last_killed_mob_island}.
     */
    @SerializedName("last_killed_mob_island")
    private String lastKilledMobIsland;

    /**
     * One kill counted toward the current vampire slayer spawn.
     */
    @Getter
    public static class MobKill {

        /**
         * Combat experience this kill contributed.
         */
        private int xp;

        /**
         * When the kill happened. {@link SkyBlockDate.RealTime} binds the wire's raw epoch
         * milliseconds, which is a different timestamp type from the quest start inherited from
         * {@link SlayerQuest} even though both read the same wire shape.
         */
        private SkyBlockDate.RealTime timestamp;

    }

}
