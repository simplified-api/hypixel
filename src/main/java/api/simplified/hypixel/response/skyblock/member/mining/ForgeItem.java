package api.simplified.hypixel.response.skyblock.member.mining;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.Item;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * One item being smelted in a slot of The Forge, the refining area of the Dwarven Mines.
 *
 * <p>
 * A forge process occupies its slot for a real-time duration measured from the moment it started, so
 * the record is a start stamp plus the slot it holds; it leaves the wire once the finished item has
 * been collected. Durations run in real time rather than SkyBlock time and the Quick Forge perk
 * shortens them, but only the start stamp is bound and nothing here derives the time remaining.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/The_Forge">The Forge</a>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ForgeItem {

    /**
     * The kind of process Hypixel classifies the job as, taken from the wire verbatim.
     */
    private String type;

    /**
     * The SkyBlock item id being forged.
     *
     * <p>
     * Bound from the wire's {@code id} and renamed, because {@code id} alone reads as an id for the
     * process rather than for the item coming out of it.
     */
    @SerializedName("id")
    private String itemId;

    /**
     * When the process was queued.
     *
     * <p>
     * The wire key is {@code startTime} - camelCase where everything around it is snake_case, which
     * is why the name has to be declared.
     */
    @SerializedName("startTime")
    private SkyBlockDate.RealTime started;

    /**
     * The forge slot the process occupies.
     */
    private int slot;

    /**
     * Whether the member has already been told the item finished.
     */
    private boolean notified;

    /**
     * The item being forged, resolved out of the {@link Item} repository by its id.
     *
     * <p>
     * This is the resolved layer: it needs an open {@link SkyBlockData} session and throws without
     * one. An id the repository does not carry comes back {@code null} in spite of the declared
     * {@link NotNull}.
     */
    public @NotNull Item getItem() {
        return SkyBlockData.getRepository(Item.class).findFirstOrNull(Item::getId, this.getItemId());
    }

}