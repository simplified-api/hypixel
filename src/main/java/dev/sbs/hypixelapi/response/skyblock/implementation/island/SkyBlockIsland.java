package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.account.Banking;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.account.CommunityUpgrades;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.member.Member;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.profile_stats.ProfileStats;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.linked.ConcurrentLinkedMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Optional;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SkyBlockIsland {

    private static final DecimalFormat smallDecimalFormat = new DecimalFormat("#0.#");

    @SerializedName("profile_id")
    private @NotNull UUID islandId;
    @SerializedName("community_upgrades")
    private @NotNull Optional<CommunityUpgrades> communityUpgrades = Optional.empty();
    private @NotNull Optional<Banking> banking = Optional.empty();
    @SerializedName("game_mode")
    private @NotNull Optional<String> gameMode = Optional.empty();
    @SerializedName("cute_name")
    private @NotNull Optional<String> profileName = Optional.empty();
    private boolean selected;
    private @NotNull ConcurrentLinkedMap<UUID, Member> members = Concurrent.newLinkedMap();

    /**
     * Wraps this class with database access.
     * <br><br>
     * Requires an active database session.
     */
    public @NotNull EnhancedSkyBlockIsland asEnhanced() {
        return new EnhancedSkyBlockIsland(this);
    }

    public @NotNull ProfileStats getProfileStats(@NotNull Member member) {
        return this.getProfileStats(member, true);
    }

    public @NotNull ProfileStats getProfileStats(@NotNull Member member, boolean calculateBonus) {
        return new ProfileStats(this, member.asEnhanced(), calculateBonus);
    }

    public boolean hasMember(@NotNull UUID uniqueId) {
        return this.getMembers().containsKey(uniqueId);
    }

}
