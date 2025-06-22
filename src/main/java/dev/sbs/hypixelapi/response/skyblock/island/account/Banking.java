package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.account;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class Banking {

    private double balance;
    private @NotNull ConcurrentList<Transaction> transactions = Concurrent.newList();

    @Getter
    public static class Transaction {

        private double amount;
        private SkyBlockDate.RealTime timestamp;
        private Action action;
        @Getter(AccessLevel.NONE)
        @SerializedName("initiator_name")
        private String initiatorName;

        public String getInitiatorName() {
            return this.initiatorName.replace("Â", ""); // API Artifact
        }

        public enum Action {

            WITHDRAW,
            DEPOSIT

        }

    }

}
