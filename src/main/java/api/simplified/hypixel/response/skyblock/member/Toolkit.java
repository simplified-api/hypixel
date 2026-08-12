package api.simplified.hypixel.response.skyblock.member;

import api.simplified.hypixel.common.NbtContent;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * A menu unlocked by a consumable that holds a set of specialised tools and tracks which of them is
 * in use in which slot.
 *
 * <p>
 * One class covers both the farming toolkit and the hunting toolkit, and the wire ships the same
 * shape for each.
 */
@Getter
public class Toolkit {

    /**
     * Whether the toolkit menu is unlocked, bound from the uppercase wire key {@code IS_UNLOCKED}.
     */
    @Accessors(fluent = true)
    @SerializedName("IS_UNLOCKED")
    private boolean isUnlocked;

    /**
     * Whether each tool is in use, keyed by tool id and then by slot index. The slot indices arrive
     * as JSON object keys and bind as numbers.
     */
    @SerializedName("IN_USE")
    private @NotNull ConcurrentMap<String, ConcurrentMap<Integer, Boolean>> inUse = Concurrent.newMap();

    /**
     * The item stacks stored for each tool, keyed by tool id, folding in every remaining key on the
     * node. The filter is open, so {@code IS_UNLOCKED} and {@code IN_USE} stay out of this map only
     * because the two named fields claim them first.
     */
    @Capture
    private @NotNull ConcurrentMap<String, ConcurrentList<NbtContent>> tools = Concurrent.newMap();

    /**
     * Reads the item stacks stored for one tool.
     *
     * @param toolId the tool id to read
     * @return the stacks stored for that tool, empty for an id the toolkit does not hold
     */
    public @NotNull ConcurrentList<NbtContent> getTool(@NotNull String toolId) {
        return this.getTools().getOrDefault(toolId, Concurrent.newUnmodifiableList());
    }

    /**
     * Checks whether one tool is in use in one of its slots.
     *
     * @param toolId the tool id to check
     * @param slot the slot index to check
     * @return whether the tool is in use there, false for a tool or slot the toolkit does not hold
     */
    public boolean isInUse(@NotNull String toolId, int slot) {
        return this.getInUse()
            .getOrDefault(toolId, Concurrent.newUnmodifiableMap())
            .getOrDefault(slot, false);
    }

}
