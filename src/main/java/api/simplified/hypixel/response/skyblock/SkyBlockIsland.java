package api.simplified.hypixel.response.skyblock;

import api.simplified.hypixel.response.skyblock.island.Banking;
import api.simplified.hypixel.response.skyblock.island.CommunityUpgrades;
import api.simplified.hypixel.response.skyblock.island.GameMode;
import api.simplified.hypixel.response.skyblock.island.Profile;
import api.simplified.hypixel.response.skyblock.stats.ProfileStats;
import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentLinkedMap;
import dev.simplified.collection.ConcurrentList;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * One SkyBlock profile - the island a player and their co-op members share, and every member on it.
 * <p>
 * A player holds several profiles at once and plays one of them at a time; each is named after a
 * fruit and carries its own members, bank and upgrades. What a member exposes here is theirs to
 * decide: a subtree a member has hidden through their in-game API settings simply does not arrive,
 * which is why every collection defaults non-null rather than reaching a caller as null.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Profile">Profile</a>
 */
@Getter
@NoArgsConstructor
public class SkyBlockIsland {

    /**
     * Unique id of the profile.
     */
    @SerializedName("profile_id")
    private @NotNull UUID islandId;

    /**
     * When the profile was made, absent on profiles older than the field itself.
     */
    @SerializedName("created_at")
    private @NotNull Optional<SkyBlockDate.RealTime> createdAt = Optional.empty();

    /**
     * Island-wide upgrades bought from the community shop, absent until the first one is started.
     */
    @SerializedName("community_upgrades")
    private @NotNull Optional<CommunityUpgrades> communityUpgrades = Optional.empty();

    /**
     * The island's shared bank account and its transaction history, absent while no bank is open.
     */
    private @NotNull Optional<Banking> banking = Optional.empty();

    /**
     * Game mode the profile is played under, {@link GameMode#CLASSIC} when the wire names none.
     */
    @SerializedName("game_mode")
    private @NotNull GameMode gameMode = GameMode.CLASSIC;

    /**
     * Fruit name the profile is shown by in game.
     */
    @SerializedName("cute_name")
    private @NotNull Profile profile;

    /**
     * Whether this is the profile the player last played.
     */
    private boolean selected;

    /**
     * Every member of the island keyed by their unique id, in the order the wire listed them.
     */
    private @NotNull ConcurrentLinkedMap<UUID, SkyBlockMember> members = Concurrent.newLinkedMap();

    /**
     * Reads whether a player is a member of the island.
     *
     * @param uniqueId the player's unique id
     * @return true when the island carries a member under that id
     */
    public boolean hasMember(@NotNull UUID uniqueId) {
        return this.getMembers().containsKey(uniqueId);
    }

    /**
     * Collection totals for the whole island, derived by summing every member's own totals.
     */
    public @NotNull ConcurrentLinkedMap<String, Long> getCollection() {
        return this.getMembers()
            .stream()
            .flatMap((uniqueId, member) -> member.getCollection().stream())
            .collect(Concurrent.toLinkedMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Long::sum
            ));
    }

    /**
     * Unlocked collection tiers for the whole island, derived by summing every member's own tiers.
     */
    public @NotNull ConcurrentLinkedMap<String, Integer> getCollectionUnlocked() {
        return this.getMembers()
            .stream()
            .flatMap((uniqueId, member) -> member.getCollectionUnlocked().stream())
            .collect(Concurrent.toLinkedMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                Integer::sum
            ));
    }

    /**
     * Collects the minion tiers the island has crafted for one minion type.
     *
     * @param itemId the minion's item id
     * @return the distinct tiers crafted across every member, in ascending order
     */
    public @NotNull ConcurrentList<Integer> getCraftedMinions(@NotNull String itemId) {
        return this.getMembers()
            .stream()
            .values()
            .flatMap(member -> member.getCraftedMinions(itemId).stream())
            .distinct()
            .collect(Concurrent.toUnmodifiableList())
            .sorted(Comparator.naturalOrder());
    }

    /**
     * Builds one member's combat stats against this island, counting their bonuses.
     *
     * @param member the member to measure
     * @return the member's stats on this island
     */
    public @NotNull ProfileStats getProfileStats(@NotNull SkyBlockMember member) {
        return this.getProfileStats(member, true);
    }

    /**
     * Builds one member's combat stats against this island.
     *
     * @param member the member to measure
     * @param calculateBonus whether to fold the member's bonuses into the totals
     * @return the member's stats on this island
     */
    public @NotNull ProfileStats getProfileStats(@NotNull SkyBlockMember member, boolean calculateBonus) {
        return ProfileStats.compute(this, member, calculateBonus);
    }

    /**
     * Minion slots the island has earned, derived from every member's crafted minions together with
     * the minion slot community upgrades bought.
     */
    public int getUniqueMinions() {
        return this.getMembers()
            .stream()
            .values()
            .mapToInt(member -> member.getPlayerData().getCraftedMinions().size())
            .sum() + this.getCommunityUpgrades()
            .map(communityUpgrades -> communityUpgrades.getUpgraded()
                .stream()
                .filter(upgraded -> upgraded.getUpgrade() == CommunityUpgrades.Type.MINION_SLOTS)
                .count()
            )
            .map(Long::intValue)
            .orElse(0);
    }

}
