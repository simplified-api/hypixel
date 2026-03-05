package dev.sbs.minecraftapi.client.hypixel.response.skyblock.election;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor
public class Candidate {

    private @NotNull String key = "";
    private @NotNull String name = "";
    @SerializedName(alternate = "perk", value = "perks")
    private @NotNull ConcurrentList<Perk> perks = Concurrent.newList();

    @Getter
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    public static class Perk {

        private @NotNull String name = "";
        private @NotNull String description = "";
        private boolean minister;

    }

}
