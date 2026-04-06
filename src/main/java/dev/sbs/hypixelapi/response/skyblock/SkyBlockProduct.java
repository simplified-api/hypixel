package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class SkyBlockProduct {

    @SerializedName("product_id")
    private @NotNull String itemId;
    @SerializedName("buy_summary")
    private @NotNull ConcurrentList<Summary> buySummary = Concurrent.newList();
    @SerializedName("sell_summary")
    private @NotNull ConcurrentList<Summary> sellSummary = Concurrent.newList();
    @SerializedName("quick_status")
    private @NotNull Status quickStatus = new Status();

    @Getter
    public static class Status {

        private String productId;
        private double sellPrice;
        private long sellVolume;
        private long sellMovingWeek;
        private long sellOrders;
        private double buyPrice;
        private long buyVolume;
        private long buyMovingWeek;
        private long buyOrders;

    }

    @Getter
    public static class Summary {

        private long amount;
        private double pricePerUnit;
        @SerializedName("orders")
        private int numberOfOrders;

    }

}
