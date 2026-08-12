package api.simplified.hypixel.response.skyblock.member;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A member's progress on the Experimentation Table.
 *
 * <p>
 * The table is furniture that runs three daily minigames for enchanting experience and enchanted
 * books. The wire names them {@code pairings}, {@code simon} and {@code numbers}, none of which is
 * the name the game gives them.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Experimentation_Table">Experimentation Table</a>
 */
@Getter
public class Experimentation {

    /**
     * Daily-reset claims the member has used.
     */
    @SerializedName("claims_resets")
    private int resetClaims;

    /**
     * When the reset claim last ran.
     */
    @SerializedName("claims_resets_timestamp")
    private Optional<SkyBlockDate.RealTime> resetClaimsAt = Optional.empty();

    /**
     * Experience serums drunk.
     */
    @SerializedName("serums_drank")
    private int serumsDrank;

    /**
     * When the table last ticked its charge.
     */
    @SerializedName("charge_track_timestamp")
    private Optional<SkyBlockDate.RealTime> chargeTrackAt = Optional.empty();

    /**
     * Whether the one-off retroactive RNG meter payout has been taken.
     */
    @SerializedName("claimed_retroactive_rng")
    private boolean claimedRetroactiveRngMeter;

    /**
     * The Superpairs table, bound from the wire's {@code pairings}.
     */
    @SerializedName("pairings")
    private @NotNull Table superpairs = new Table();

    /**
     * The Chronomatron table, bound from the wire's {@code simon} because the game is Simon Says.
     */
    @SerializedName("simon")
    private @NotNull Table chronomatron = new Table();

    /**
     * The Ultrasequencer table, bound from the wire's {@code numbers}.
     */
    @SerializedName("numbers")
    private @NotNull Table ultrasequencer = new Table();

    /**
     * One of the three Experimentation Table minigames and a member's record on it.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Experiments">Experiments</a>
     */
    @Getter
    public static class Table {

        /**
         * When the game was last played, defaulting to the epoch rather than to null.
         */
        @SerializedName("last_attempt")
        private @NotNull SkyBlockDate.RealTime lastAttempt = new SkyBlockDate.RealTime(0);

        /**
         * When a reward was last claimed, defaulting to the epoch rather than to null.
         */
        @SerializedName("last_claimed")
        private @NotNull SkyBlockDate.RealTime lastClaimed = new SkyBlockDate.RealTime(0);

        /**
         * Bonus clicks banked for the next run.
         */
        @SerializedName("bonus_clicks")
        private int bonusClicks;

        /**
         * Whether the current run's reward has been taken.
         */
        private boolean claimed;

        /**
         * Attempts made per difficulty tier, meant to fold in every {@code attempts_N} wire key.
         */
        @Capture(filter = "^attempts_")
        private @NotNull ConcurrentMap<Integer, Integer> attempts = Concurrent.newMap();

        /**
         * Rewards claimed per difficulty tier, meant to fold in every {@code claims_N} wire key.
         * <p>
         * The root's {@code claims_resets} only shares the
         * prefix and belongs to no capture.
         */
        @Capture(filter = "^claims_")
        private @NotNull ConcurrentMap<Integer, Integer> claims = Concurrent.newMap();

        /**
         * Best score reached per difficulty tier, meant to fold in every {@code best_score_N} wire key.
         */
        @Capture(filter = "^best_score_")
        private @NotNull ConcurrentMap<Integer, Integer> bestScore = Concurrent.newMap();

    }

}
