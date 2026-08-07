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

@Getter
public class Toolkit {

    @Accessors(fluent = true)
    @SerializedName("IS_UNLOCKED")
    private boolean isUnlocked;
    @SerializedName("IN_USE")
    private @NotNull ConcurrentMap<String, ConcurrentMap<Integer, Boolean>> inUse = Concurrent.newMap();
    @Capture
    private @NotNull ConcurrentMap<String, ConcurrentList<NbtContent>> tools = Concurrent.newMap();

    public @NotNull ConcurrentList<NbtContent> getTool(@NotNull String toolId) {
        return this.getTools().getOrDefault(toolId, Concurrent.newUnmodifiableList());
    }

    public boolean isInUse(@NotNull String toolId, int slot) {
        return this.getInUse()
            .getOrDefault(toolId, Concurrent.newUnmodifiableMap())
            .getOrDefault(slot, false);
    }

}
