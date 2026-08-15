package api.simplified.hypixel.response.skyblock.member.rift;

import api.simplified.hypixel.common.NbtContent;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * The second set of containers the Rift keeps for a member.
 *
 * <p>
 * A member's normal inventory, armour, equipment and ender chest are left behind at the portal and a
 * Rift-only set is carried instead. Each container arrives as one base64 NBT blob and stays a string
 * until a caller asks {@link NbtContent} to decode it - nothing here decodes anything at bind time,
 * and every container defaults to an empty blob so an untouched one parses rather than throwing.
 */
@Getter
public class RiftInventory {

    /**
     * The Rift inventory's own slots as a base64 NBT blob, bound from {@code inv_contents}.
     */
    @SerializedName("inv_contents")
    private @NotNull NbtContent inventory = new NbtContent();

    /**
     * The worn Rift armour slots, bound from {@code inv_armor}.
     */
    @SerializedName("inv_armor")
    private @NotNull NbtContent armor = new NbtContent();

    /**
     * The Rift's own ender chest, bound from {@code ender_chest_contents}.
     */
    @SerializedName("ender_chest_contents")
    private @NotNull NbtContent enderChest = new NbtContent();

    /**
     * The Rift equipment slots - necklace, cloak, belt and gloves - bound from
     * {@code equipment_contents}.
     */
    @SerializedName("equipment_contents")
    private @NotNull NbtContent equipment = new NbtContent();

    /**
     * One entry per Rift ender chest page, bound from {@code ender_chest_page_icons}. The size is
     * the page count, not the icon count - the wire sends a literal null for a page with no icon
     * set, which is why each element is an {@link Optional} rather than an {@link NbtContent}.
     */
    @SerializedName("ender_chest_page_icons")
    private @NotNull ConcurrentList<Optional<NbtContent>> enderChestPageIcons = Concurrent.newList();

}
