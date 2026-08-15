package api.simplified.hypixel.response.skyblock.member;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;

/**
 * A member's progress on Trevor the Trapper's quest.
 *
 * <p>
 * Trevor hands out a quest to track and kill one animal. Finishing it pays pelts, which buy items
 * from the shop in the Trapper's Den, and the kills feed the bestiary.
 *
 * <p>
 * The wire nests this node under {@code quests}, so a member the wire sends no quests for keeps a
 * default instance.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Trevor">Trevor</a>
 */
@Getter
public class Trapper {

    /**
     * When the last quest was handed out, null until the wire supplies one.
     */
    @SerializedName("last_task_time")
    private SkyBlockDate.RealTime lastTask;

    /**
     * Pelts held.
     */
    @SerializedName("pelt_count")
    private int peltCount;

}
