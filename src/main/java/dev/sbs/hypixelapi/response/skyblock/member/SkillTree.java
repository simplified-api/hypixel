package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.Capture;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Getter
public class SkillTree {

    @SerializedName("nodes")
    private @NotNull ConcurrentMap<String, Skill> nodes = Concurrent.newMap();
    @SerializedName("selected_ability")
    private @NotNull ConcurrentMap<String, String> selectedAbility = Concurrent.newMap();
    @SerializedName("tokens_spent")
    private @NotNull ConcurrentMap<String, Integer> spentTokens = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Long> experience = Concurrent.newMap();
    @SerializedName("last_reset")
    private @NotNull ConcurrentMap<String, Instant> lastReset = Concurrent.newMap();
    @SerializedName("refund_ability_free")
    private boolean refundAbilityFree;

    @Getter
    @NoArgsConstructor
    public static class Skill {

        @Capture
        private @NotNull ConcurrentMap<String, Node> entries = Concurrent.newMap();

    }

    @Getter
    @NoArgsConstructor
    public static class Node {

        @SerializedName("")
        private int level;
        @SerializedName("toggle_")
        private boolean enabled = true;

    }

}
