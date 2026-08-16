package api.simplified.hypixel.response.skyblock.island;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.annotations.RequiredArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Optional;

/**
 * The upgrades a profile has bought from the Community Shop, and the one it is paying for now.
 * <p>
 * Every finished tier stays in the history as its own entry, so an upgrade bought to tier three
 * appears three times rather than once.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Account_%26_Profile_Upgrades">Account &amp; Profile Upgrades</a>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommunityUpgrades {

    /**
     * Upgrade the profile is paying for right now, empty when none is in progress.
     */
    @SerializedName("currently_upgrading")
    private @NotNull Optional<Upgrading> currentlyUpgrading = Optional.empty();

    /**
     * Every upgrade tier the profile has finished, one entry per tier, bound from
     * {@code upgrade_states}.
     */
    @SerializedName("upgrade_states")
    private @NotNull ConcurrentList<Upgraded> upgraded = Concurrent.newList();

    /**
     * Finds the highest tier of one upgrade the profile has finished.
     *
     * @param type the upgrade to look for
     * @return the highest finished tier, or {@code 0} when the profile has bought none of it
     */
    public int getHighestTier(@NotNull Type type) {
        return this.getUpgraded()
            .stream()
            .filter(upgraded -> upgraded.getUpgrade().name().equalsIgnoreCase(type.name()))
            .sorted((o1, o2) -> Comparator.comparing(Upgraded::getTier).compare(o2, o1))
            .map(Upgraded::getTier)
            .findFirst()
            .orElse(0);
    }

    /**
     * Collects every finished tier of one upgrade, lowest tier first.
     *
     * @param type the upgrade to look for
     * @return the finished tiers of that upgrade, empty when the profile has bought none of it
     */
    public @NotNull ConcurrentList<Upgraded> getUpgrades(@NotNull Type type) {
        return this.getUpgraded()
            .stream()
            .filter(upgraded -> upgraded.getUpgrade().name().equalsIgnoreCase(type.name()))
            .sorted((o1, o2) -> Comparator.comparing(Upgraded::getTier).compare(o1, o2))
            .collect(Concurrent.toList());
    }

    /**
     * One upgrade tier the profile has finished paying for.
     */
    @Getter
    public static class Upgraded extends Upgrading {

        /**
         * Real time the finished tier was claimed.
         */
        @SerializedName("claimed_ms")
        private SkyBlockDate.RealTime claimed;

        /**
         * Universally unique id of the member that claimed the finished tier.
         */
        @SerializedName("claimed_by")
        private String claimedBy;

        /**
         * Whether the tier's wait was fast tracked with gems, bound from the one-word
         * {@code fasttracked}.
         */
        @SerializedName("fasttracked")
        private boolean fastTracked;

    }

    /**
     * One upgrade tier the profile has started paying for.
     */
    @Getter
    public static class Upgrading {

        /**
         * Upgrade being bought.
         */
        private Type upgrade;

        /**
         * Tier being bought, bound from {@code tier} or from the {@code new_tier} the wire spells
         * an in-progress upgrade with.
         */
        @SerializedName(alternate = "new_tier", value = "tier")
        private int tier;

        /**
         * Real time the tier was started.
         */
        @SerializedName("started_ms")
        private SkyBlockDate.RealTime started;

        /**
         * Universally unique id of the member that started the tier, bound from {@code started_by}
         * or from the {@code who_started} the wire spells it with elsewhere.
         */
        @SerializedName(alternate = "who_started", value = "started_by")
        private String startedBy;

    }

    /**
     * The five upgrades the Community Shop sells for a profile.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Account_%26_Profile_Upgrades">Account &amp; Profile Upgrades</a>
     */
    @Getter
    @RequiredArgsConstructor
    public enum Type {

        /**
         * Extra minion slots on the island, shared by every member of the profile.
         */
        @SerializedName("minion_slots")
        MINION_SLOTS(5),

        /**
         * Daily coin bonus each member collects on logging in.
         */
        @SerializedName("coins_allowance")
        COINS_ALLOWANCE(5),

        /**
         * Extra guests that can visit the island at once.
         */
        @SerializedName("guests_count")
        GUESTS_COUNT(5),

        /**
         * Extra width the private island is built out to.
         */
        @SerializedName("island_size")
        ISLAND_SIZE(10),

        /**
         * Extra members that can join the profile's co-op.
         */
        @SerializedName("coop_slots")
        COOP_SLOTS(3);

        /**
         * Highest tier this upgrade can be bought to.
         */
        private final int maxLevel;

        /**
         * Display name of the upgrade, the constant's name in title case.
         */
        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

    }

}
