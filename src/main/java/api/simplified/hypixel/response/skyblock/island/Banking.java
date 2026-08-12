package api.simplified.hypixel.response.skyblock.island;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * The profile bank, shared by every member of the profile.
 * <p>
 * The whole subtree is absent when a member has the bank switched off in their API settings, which
 * is a privacy setting rather than an error.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Bank">Bank</a>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Banking {

    /**
     * Coins held in the profile bank.
     */
    private double balance;

    /**
     * Deposits and withdrawals the wire reports against the account.
     */
    private @NotNull ConcurrentList<Transaction> transactions = Concurrent.newList();

    /**
     * One movement of coins into or out of the profile bank.
     */
    @Getter
    public static class Transaction {

        /**
         * Coins moved by this transaction.
         */
        private double amount;

        /**
         * Real time the transaction was recorded.
         */
        private SkyBlockDate.RealTime timestamp;

        /**
         * Direction the coins moved.
         */
        private Action action;

        /**
         * Colour-coded name of whoever moved the coins, exactly as the wire spells it.
         */
        @Getter(AccessLevel.NONE)
        @SerializedName("initiator_name")
        private String initiatorName;

        /**
         * Name of whoever moved the coins - a member of the profile, or {@code Bank Interest} for
         * an interest payout - with the stray {@code Â} left in front of each {@code §} by the
         * wire's mis-encoded colour codes stripped out. The colour code itself is kept.
         */
        public String getInitiatorName() {
            return this.initiatorName.replace("Â", ""); // API Artifact
        }

        /**
         * The two directions coins move in the bank.
         */
        public enum Action {

            /**
             * Coins taken out of the bank.
             */
            WITHDRAW,

            /**
             * Coins put into the bank, including the interest the bank pays itself.
             */
            DEPOSIT

        }

    }

}
