package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.slayer;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.reflection.Reflection;
import lombok.Getter;

import java.util.Map;

public class SlayerBoss implements PostInit {

    private final static Reflection<SlayerBoss> slayerRef = Reflection.of(SlayerBoss.class);
    @Getter protected Slayer.Type type;
    @SerializedName("xp")
    @Getter protected double experience;

    protected ConcurrentMap<String, Boolean> claimed_levels = Concurrent.newMap(); // level_#: true
    protected int boss_kills_tier_0;
    protected int boss_kills_tier_1;
    protected int boss_kills_tier_2;
    protected int boss_kills_tier_3;
    protected int boss_kills_tier_4;
    protected boolean initialized;

    @Getter protected ConcurrentMap<Integer, Boolean> claimed;
    @Getter protected ConcurrentMap<Integer, Boolean> claimedSpecial;
    @Getter protected ConcurrentMap<Integer, Integer> kills;

    @Override
    @SuppressWarnings("all")
    public void postInit() {
        ConcurrentMap<Integer, Boolean> claimed = Concurrent.newMap();
        ConcurrentMap<Integer, Boolean> claimedSpecial = Concurrent.newMap();
        ConcurrentMap<Integer, Integer> kills = Concurrent.newMap();

        for (Map.Entry<String, Boolean> entry : this.claimed_levels.entrySet()) {
            String entryKey = entry.getKey().replace("level_", "");
            boolean special = entryKey.endsWith("_special");
            entryKey = special ? entryKey.replace("_special", "") : entryKey;
            (special ? claimedSpecial : claimed).put(Integer.parseInt(entryKey), entry.getValue());
        }

        for (int i = 0; i < 5; i++)
            kills.put(i + 1, (int) slayerRef.getValue(String.format("boss_kills_tier_%s", i), this));

        this.claimed = claimed.toUnmodifiableMap();
        this.claimedSpecial = claimedSpecial.toUnmodifiableMap();
        this.kills = kills.toUnmodifiableMap();
        this.initialized = true;
    }

    public boolean isClaimed(int level) {
        return this.getClaimed().getOrDefault(level, false);
    }

}
