package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Map;

@Getter
public class SkillTree implements PostInit {

    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, ConcurrentMap<String, Object>> nodesMap = Concurrent.newMap();
    private transient @NotNull ConcurrentMap<String, ConcurrentList<Node>> nodes = Concurrent.newMap();
    @SerializedName("selected_ability")
    private @NotNull ConcurrentMap<String, String> selectedAbility = Concurrent.newMap();
    @SerializedName("tokens_spent")
    private @NotNull ConcurrentMap<String, String> spentTokens = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Long> experience = Concurrent.newMap();
    @SerializedName("last_reset")
    private @NotNull ConcurrentMap<String, Instant> lastReset = Concurrent.newMap();
    @SerializedName("refund_ability_free")
    private boolean refundAbilityFree;

    @Override
    public void postInit() {
        this.nodes = this.nodesMap.stream()
            .mapValue(SkillTree::buildList)
            .collect(Concurrent.toUnmodifiableMap());
    }

    private static @NotNull ConcurrentList<Node> buildList(@NotNull ConcurrentMap<String, Object> originMap) {
        return originMap.stream()
            .filterValue(Integer.class::isInstance)
            .mapValue(Integer.class::cast)
            .collapseToSingle((id, level) -> new SkillTree.Node(
                id,
                level,
                originMap.stream()
                    .filterKey(subId -> subId.startsWith("toggle_"))
                    .filterKey(subId -> subId.endsWith(id))
                    .filterValue(Boolean.class::isInstance)
                    .mapValue(Boolean.class::cast)
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(true)
            ))
            .collect(Concurrent.toUnmodifiableList());
    }

    @Getter
    @AllArgsConstructor
    public static class Node {

        private final @NotNull String id;
        private final int level;
        private final boolean enabled;

    }

}
