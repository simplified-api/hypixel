package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class Experimentation implements PostInit {

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

    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, Object> pairings = Concurrent.newMap();
    private transient Table superpairs;

    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, Object> simon = Concurrent.newMap();
    private transient Table chronomatron;

    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, Object> numbers = Concurrent.newMap();
    private transient Table ultrasequencer;

    @Override
    public void postInit() {
        this.superpairs = new Table(this.pairings);
        this.chronomatron = new Table(this.simon);
        this.ultrasequencer = new Table(this.numbers);
    }

    @Getter
    public static class Table {

        private final @NotNull SkyBlockDate.RealTime lastAttempt;
        private final @NotNull SkyBlockDate.RealTime lastClaimed;
        private final int bonusClicks;
        private final boolean claimed;
        private final @NotNull ConcurrentMap<Integer, Integer> attempts;
        private final @NotNull ConcurrentMap<Integer, Integer> claims;
        private final @NotNull ConcurrentMap<Integer, Integer> bestScore;

        private Table(@NotNull ConcurrentMap<String, Object> tableData) {
            ConcurrentMap<String, Object> tableDataMap = Concurrent.newMap(tableData);
            this.lastAttempt = new SkyBlockDate.RealTime((long) tableDataMap.removeOrGet("last_attempt", 0L));
            this.lastClaimed = new SkyBlockDate.RealTime((long) tableDataMap.removeOrGet("last_claimed", 0L));
            this.bonusClicks = (int) tableDataMap.removeOrGet("bonus_clicks", 0);
            this.claimed = (boolean) tableDataMap.removeOrGet("claimed", false);

            ConcurrentMap<String, ConcurrentMap<Integer, Integer>> filteredData = Concurrent.newMap();

            tableDataMap.forEach((key, value) -> {
                if (!filteredData.containsKey(key))
                    filteredData.put(key, Concurrent.newMap());

                String actual = key.substring(0, key.lastIndexOf("_"));
                filteredData.get(key).put(Integer.parseInt(key.replace(String.format("%s_", actual), "")), (int) value);
            });

            this.attempts = filteredData.removeOrGet("attempts", Concurrent.newMap());
            this.claims = filteredData.removeOrGet("claims", Concurrent.newMap());
            this.bestScore = filteredData.removeOrGet("best_score", Concurrent.newMap());
        }

    }

}
