package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.foraging;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class Temples {

    @SerializedName("unlocked_temples")
    private @NotNull ConcurrentList<String> unlockedTemples = Concurrent.newList();

}
