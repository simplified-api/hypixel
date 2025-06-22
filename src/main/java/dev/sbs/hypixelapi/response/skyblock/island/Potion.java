package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
public class Potion {

    private String effect;
    private int level;
    @SerializedName("ticks_remaining")
    private int remainingTicks;
    private boolean infinite;
    private @NotNull ConcurrentList<Modifier> modifiers = Concurrent.newList();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Modifier {

        private String key;
        @SerializedName("amp")
        private int amplifier;

    }

}
