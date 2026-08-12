package api.simplified.hypixel.response.skyblock.member.foraging;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A member's Heart of the Forest - the Foraging skill tree and the counterpart to the Heart of the
 * Mountain.
 *
 * <p>
 * It is bought with forest whispers earned from tree gifts on Galatea and from Starlyn contests, and
 * is bound from {@code foraging_core}: the whisper ledger, the Lottery perk's daily effect and the
 * daily tree-cutting counters.
 *
 * <p>
 * As on the mining side, the perk tree itself is bound from a separate {@code skill_tree} node and
 * is not reachable from here. Several of the fields below are not currently sent on this node at
 * all, and bind their defaults in silence.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Heart_of_the_Forest">Heart of the Forest</a>
 */
@Getter
public class HeartOfTheForest {

    /**
     * The buff the Lottery perk rolled for the current SkyBlock day.
     *
     * <p>
     * Lottery is the tier 4 perk of this tree and shares its wire key with the mining tree's Sky
     * Mall. The key has not been seen on this node, so the field binds empty.
     */
    @SerializedName("current_daily_effect")
    private Optional<String> currentLotteryEffect = Optional.empty();

    /**
     * The SkyBlock day number on which the Lottery buff last rerolled.
     *
     * <p>
     * A day number rather than an epoch stamp. The key has not been seen on this node, so the field
     * binds {@code 0}.
     */
    @SerializedName("current_daily_effect_last_changed")
    private int lotteryEffectLastChanged;

    // Whispers

    /**
     * Unspent forest whispers.
     *
     * <p>
     * The key has not been seen on this node, so the field binds {@code 0}; what the wire does carry
     * is the per-pool ledger.
     */
    @SerializedName("forests_whispers")
    private int remainingForestWhispers;

    /**
     * Lifetime forest whispers spent.
     *
     * <p>
     * The key has not been seen on this node, so the field binds {@code 0}; the per-pool ledger
     * carries the spend per tier instead.
     */
    @SerializedName("forests_whispers_spent")
    private int spentForestWhispers;

    /**
     * The whisper ledger, keyed by the pool the wire names.
     */
    @SerializedName("whispers")
    private @NotNull ConcurrentMap<String, BiomeWhispers> biomeWhispers = Concurrent.newMap();

    // Daily Logs

    /**
     * Trees felled today.
     */
    @SerializedName("daily_trees_cut")
    private int dailyTreesCut;

    /**
     * The SkyBlock day the tree counter belongs to.
     *
     * <p>
     * A day number older than today means the count beside it has not been rolled over yet, so a
     * {@code 0} there is a leftover rather than today's tally.
     */
    @SerializedName("daily_trees_cut_day")
    private int dailyTreesCutDay;

    /**
     * Tree gifts opened today.
     */
    @SerializedName("daily_gifts")
    private int dailyGifts;

    /**
     * Log species cut today, towards the per-species daily bonus.
     *
     * <p>
     * A list of species ids and not a count; it can be empty on a day trees were plainly felled, so
     * an empty list does not mean no cutting happened.
     */
    @SerializedName("daily_log_cut")
    private @NotNull ConcurrentList<String> dailyLogCut = Concurrent.newList();

    /**
     * The SkyBlock day the log species list belongs to.
     */
    @SerializedName("daily_log_cut_day")
    private int dailyLogCutDay;

    /**
     * One pool of forest whispers - a lifetime total plus the amount spent at each tier of the tree
     * it feeds.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Forest_Whispers">Forest Whispers</a>
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BiomeWhispers {

        /**
         * Lifetime whispers earned into this pool.
         *
         * <p>
         * Not the sum of the tier spends: the unspent balance is the difference between the two, and
         * nothing here derives it. Being a declared field is also the only thing keeping this key
         * out of the captured tier map.
         */
        private int total;

        /**
         * Whisper spend per tier, keyed by the tier number.
         *
         * <p>
         * A catch-all capture, so every key on the pool that is not declared lands here. The wire's
         * keys are strings and convert to the numeric key type; one that cannot convert is diverted
         * to overflow rather than collapsing onto a null key. The map itself is not reachable from
         * outside - {@link #getSpent(int)} is the only way in.
         */
        @Getter(AccessLevel.NONE)
        @Capture
        private @NotNull ConcurrentMap<Integer, Tier> tiers = Concurrent.newMap();

        /**
         * Reads the whispers spent at one tier of this pool.
         *
         * <p>
         * A tier the pool has no entry for and a tier with nothing spent are indistinguishable, as
         * both answer zero.
         *
         * @param tier the tier number to read
         * @return the whispers spent at that tier, or {@code 0} when the pool holds no entry for it
         */
        public int getSpent(int tier) {
            return Optional.ofNullable(this.tiers.get(tier))
                .map(Tier::getSpent)
                .orElse(0);
        }

        /**
         * One tier's whisper spend.
         */
        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Tier {

            /**
             * Whispers spent at this tier.
             */
            private int spent;

        }

    }

}
