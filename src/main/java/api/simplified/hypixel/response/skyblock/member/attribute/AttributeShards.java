package api.simplified.hypixel.response.skyblock.member.attribute;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.gson.annotation.SerializedPath;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class AttributeShards {

    @SerializedPath("traps.active_traps")
    private @NotNull ConcurrentList<ActiveTrap> activeTraps = Concurrent.newList();
    @SerializedName("owned")
    private @NotNull ConcurrentList<AttributeShard> ownedShards = Concurrent.newList();
    private int fused;

}
