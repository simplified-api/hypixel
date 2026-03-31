package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.Capture;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class Experimentation {

    @SerializedName("claims_resets")
    private int resetClaims;
    @SerializedName("claims_resets_timestamp")
    private Optional<SkyBlockDate.RealTime> resetClaimsAt = Optional.empty();
    @SerializedName("serums_drank")
    private int serumsDrank;
    @SerializedName("charge_track_timestamp")
    private Optional<SkyBlockDate.RealTime> chargeTrackAt = Optional.empty();
    @SerializedName("claimed_retroactive_rng")
    private boolean claimedRetroactiveRngMeter;

    @SerializedName("pairings")
    private @NotNull Table superpairs = new Table();
    @SerializedName("simon")
    private @NotNull Table chronomatron = new Table();
    @SerializedName("numbers")
    private @NotNull Table ultrasequencer = new Table();

    @Getter
    public static class Table {

        @SerializedName("last_attempt")
        private @NotNull SkyBlockDate.RealTime lastAttempt = new SkyBlockDate.RealTime(0);
        @SerializedName("last_claimed")
        private @NotNull SkyBlockDate.RealTime lastClaimed = new SkyBlockDate.RealTime(0);
        @SerializedName("bonus_clicks")
        private int bonusClicks;
        private boolean claimed;
        @Capture(filter = "^attempts_")
        private transient @NotNull ConcurrentMap<Integer, Integer> attempts = Concurrent.newMap();
        @Capture(filter = "^claims_")
        private transient @NotNull ConcurrentMap<Integer, Integer> claims = Concurrent.newMap();
        @Capture(filter = "^best_score_")
        private transient @NotNull ConcurrentMap<Integer, Integer> bestScore = Concurrent.newMap();

    }

}
