package dev.sbs.minecraftapi.client.hypixel.response.skyblock.garden;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor
public class ActiveCommission {

    @SerializedName("requirement")
    private @NotNull ConcurrentList<Requirement> requirements = Concurrent.newList();
    private @NotNull Status status = Status.NOT_STARTED;
    private int position;

    @Getter
    @NoArgsConstructor
    public static class Requirement {

        @SerializedName("original_item")
        private @NotNull String baselineItem;
        @SerializedName("original_amount")
        private int baselineAmount;
        @SerializedName("item")
        private @NotNull String askedItem;
        @SerializedName("amount")
        private int askedAmount;

    }

    public enum Status {

        NOT_STARTED

    }

}
