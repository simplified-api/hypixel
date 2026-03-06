package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class PotionData {

    private String effect;
    private int level;
    @SerializedName("ticks_remaining")
    private int remainingTicks;
    private boolean infinite;
    private @NotNull ConcurrentList<Modifier> modifiers = Concurrent.newList();

    @Getter
    public static class Modifier {

        private String key;
        @SerializedName("amp")
        private int amplifier;

    }

}
