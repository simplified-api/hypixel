package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * The member's perk trees and the tokens spent in them.
 *
 * <p>
 * Two trees exist on the wire - the mining tree, which the game calls the Heart of the Mountain, and
 * the foraging tree, the Heart of the Forest. Each carries numbered extra slots an alternative build
 * can be saved into, and the slot number is a suffix on the map key rather than a field of its own,
 * so {@code foraging_5} is the fifth saved build of the foraging tree and the base build's key has
 * no suffix at all.
 *
 * <p>
 * Every field is bound; nothing here derives and nothing reaches a repository.
 */
@Getter
public class SkillTree {

    /**
     * Perks per tree slot, keyed {@code mining} or {@code foraging} with a slot suffix on the saved
     * builds.
     */
    @SerializedName("nodes")
    private @NotNull ConcurrentMap<String, Skill> nodes = Concurrent.newMap();

    /**
     * The active ability per tree, keyed the same way as the perks.
     */
    @SerializedName("selected_ability")
    private @NotNull ConcurrentMap<String, String> selectedAbility = Concurrent.newMap();

    /**
     * Tokens spent per tree slot. This node spells the trees differently from every other node here -
     * the keys are {@code mountain} and {@code forest}, not {@code mining} and {@code foraging}, and
     * the slot suffixes follow that spelling too.
     */
    @SerializedName("tokens_spent")
    private @NotNull ConcurrentMap<String, Integer> spentTokens = Concurrent.newMap();

    /**
     * Tree experience per tree, keyed the same way as the perks.
     */
    private @NotNull ConcurrentMap<String, Long> experience = Concurrent.newMap();

    /**
     * When each tree was last reset, keyed the same way as the perks.
     */
    @SerializedName("last_reset")
    private @NotNull ConcurrentMap<String, Instant> lastReset = Concurrent.newMap();

    /**
     * Whether the next respec costs nothing.
     */
    @SerializedName("refund_ability_free")
    private boolean refundAbilityFree;

    /**
     * The saved slot currently active per tree, keyed the same way as the perks.
     */
    @SerializedName("selected_skill_tree_slot")
    private @NotNull ConcurrentMap<String, Integer> selectedSlot = Concurrent.newMap();

    /**
     * SkyBlock day the last free trial was taken on.
     */
    @SerializedName("last_free_trial_day")
    private int lastFreeTrialDay;

    /**
     * The perks of one tree slot.
     */
    @Getter
    @NoArgsConstructor
    public static class Skill {

        /**
         * Perks keyed by node id. Every key of the slot's wire object is folded in, and a node id can
         * begin with a digit, so these are not identifiers.
         */
        @Capture
        private @NotNull ConcurrentMap<String, Node> entries = Concurrent.newMap();

    }

    /**
     * One perk of a tree - the level it has been taken to, and whether it is switched on.
     *
     * <p>
     * Both halves come from the wire as separate keys folded onto one entry: a node key carrying the
     * level, and its {@code toggle_}-prefixed twin carrying the switch.
     */
    @Getter
    @NoArgsConstructor
    public static class Node {

        /**
         * Level the perk has been taken to, bound from the value sitting on the bare node key.
         */
        @SerializedName("")
        private int level;

        /**
         * Whether the perk is switched on, bound from the {@code toggle_}-prefixed twin of the same
         * node key and defaulting to on when that twin is absent.
         */
        @SerializedName("toggle_")
        private boolean enabled = true;

    }

}
