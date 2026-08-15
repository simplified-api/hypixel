package api.simplified.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.annotations.RequiredArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;

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
     * Reads a tree's experience.
     *
     * @param tree the tree to read
     * @return the experience earned in it, or zero when the wire carries no entry
     */
    public long getExperience(@NotNull Tree tree) {
        return this.getExperience().getOrDefault(tree.getPerkKey(), 0L);
    }

    /**
     * Reads when a tree was last reset.
     *
     * @param tree the tree to read
     * @return when it was last reset, empty for a tree that never has been
     */
    public @NotNull Optional<Instant> getLastReset(@NotNull Tree tree) {
        return Optional.ofNullable(this.getLastReset().get(tree.getPerkKey()));
    }

    /**
     * Reads the ability a tree has equipped.
     *
     * @param tree the tree to read
     * @return the equipped ability's id, empty when none is
     */
    public @NotNull Optional<String> getSelectedAbility(@NotNull Tree tree) {
        return Optional.ofNullable(this.getSelectedAbility().get(tree.getPerkKey()));
    }

    /**
     * Reads which saved slot a tree currently has active.
     *
     * @param tree the tree to read
     * @return the active slot, defaulting to the first
     */
    public int getSelectedSlot(@NotNull Tree tree) {
        return this.getSelectedSlot().getOrDefault(tree.getPerkKey(), 1);
    }

    /**
     * Reads the tokens sunk into a tree's active slot.
     *
     * @param tree the tree to read
     * @return the tokens spent, or zero when the wire carries no entry
     */
    public int getSpentTokens(@NotNull Tree tree) {
        return this.getSpentTokens(tree, this.getSelectedSlot(tree));
    }

    /**
     * Reads the tokens sunk into one saved slot of a tree.
     *
     * @param tree the tree to read
     * @param slot the saved slot, counting from one
     * @return the tokens spent in that slot, or zero when the wire carries no entry
     */
    public int getSpentTokens(@NotNull Tree tree, int slot) {
        return this.getSpentTokens().getOrDefault(slotKey(tree.getTokenKey(), slot), 0);
    }

    /**
     * Reads the perks of a tree's active slot.
     *
     * @param tree the tree to read
     * @return that slot's perks, empty for a slot the wire carries nothing for
     */
    public @NotNull Optional<Skill> getNodes(@NotNull Tree tree) {
        return this.getNodes(tree, this.getSelectedSlot(tree));
    }

    /**
     * Reads the perks of one saved slot of a tree.
     *
     * @param tree the tree to read
     * @param slot the saved slot, counting from one
     * @return that slot's perks, empty for a slot the wire carries nothing for
     */
    public @NotNull Optional<Skill> getNodes(@NotNull Tree tree, int slot) {
        return Optional.ofNullable(this.getNodes().get(slotKey(tree.getPerkKey(), slot)));
    }

    /**
     * Builds the key one saved slot is stored under.
     * <p>
     * The first slot carries the bare key and every later one carries its number as a suffix, so the
     * base build and the saved builds sit side by side in the same map.
     *
     * @param base the tree's key in the map being read
     * @param slot the saved slot, counting from one
     * @return the key that slot is stored under
     */
    private static @NotNull String slotKey(@NotNull String base, int slot) {
        return slot <= 1 ? base : String.format("%s_%d", base, slot);
    }

    /**
     * The two perk trees, each spelled one way in most of this node and another in the token ledger.
     */
    @Getter
    @RequiredArgsConstructor
    public enum Tree {

        /**
         * The Heart of the Mountain, the mining tree.
         */
        MINING("mining", "mountain"),

        /**
         * The Heart of the Forest, the foraging tree.
         */
        FORAGING("foraging", "forest");

        /**
         * How the tree is spelled everywhere but the token ledger.
         */
        private final @NotNull String perkKey;

        /**
         * How the tree is spelled in the token ledger alone.
         */
        private final @NotNull String tokenKey;

    }

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
