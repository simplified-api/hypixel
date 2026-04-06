package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

public class ChocolateShop {

    private int year;
    private @NotNull ConcurrentList<String> rabbits = Concurrent.newList();
    @SerializedName("chocolate_spent")
    private long chocolateSpent;
    @SerializedName("cocoa_fortune_upgrades")
    private int chocolateFortune;
    @SerializedName("rabbits_purchased")
    private @NotNull ConcurrentList<String> rabbitsPurchased = Concurrent.newList();

}
