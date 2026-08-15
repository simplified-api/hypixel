package api.simplified.hypixel.response.skyblock.member.foraging;

import api.simplified.hypixel.response.skyblock.member.SkillTree;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
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
 * As on the mining side, the perk tree itself is held on the member's {@link SkillTree}, reached
 * through {@link SkillTree.Tree#FORAGING} - the perk levels, the tree's experience, the whispers
 * sunk into it and when it was last reset all sit on that node.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Heart_of_the_Forest">Heart of the Forest</a>
 */
@Getter
public class HeartOfTheForest {

    // Whispers

    /**
     * The whisper ledger, keyed by the pool the wire names.
     * <p>
     * A pool's unspent balance is its total less what it has spent across the tiers, and nothing
     * here derives it.
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
