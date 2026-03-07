package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.attribute;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class AttributeProgress {

    private @NotNull Traps traps = new Traps();
    @SerializedName("owned")
    private @NotNull ConcurrentList<AttributeShard> ownedShards = Concurrent.newList();
    private int fused;

    @Getter
    private static class Traps {

        @SerializedName("active_traps")
        private @NotNull ConcurrentList<ActiveTrap> activeTraps = Concurrent.newList();

    }

}
