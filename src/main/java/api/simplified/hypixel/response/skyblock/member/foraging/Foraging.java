package api.simplified.hypixel.response.skyblock.member.foraging;

import api.simplified.hypixel.response.skyblock.member.Toolkit;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Lenient;
import dev.simplified.gson.annotation.SerializedPath;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * A member's state on Galatea, the Foraging archipelago that opens at Foraging XII.
 *
 * <p>
 * Bound from {@code foraging}, it gathers the island's features rather than the skill itself:
 * contest and log-cutting personal bests, the Fish Family fish found so far, the hunting toolkit,
 * tree gifts opened per species, Hina's task list and the harp in front of Melody.
 *
 * <p>
 * The whole node is absent for a member who has never been to Galatea, which leaves every field on
 * its non-null default.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Foraging">Foraging</a>
 */
@Getter
public class Foraging {

    /**
     * Best scores in the Starlyn Contest and in per-log cutting, keyed by contest or log id.
     *
     * <p>
     * One map with two key vocabularies: a lowercase NPC name is a contest best, an uppercase
     * {@code *_LOG} id is a cutting best. Read from the nested {@code starlyn.personal_bests} node.
     */
    @SerializedPath("starlyn.personal_bests")
    private @NotNull ConcurrentMap<String, Integer> personalBests = Concurrent.newMap();

    /**
     * Ids of the Fish Family fish the member has found.
     *
     * <p>
     * The entries are ids rather than display names - {@code MOB_THE_FISH}, not the name shown in
     * game.
     */
    @SerializedName("fish_family")
    private @NotNull ConcurrentList<String> fishFamily = Concurrent.newList();

    /**
     * The hunting tool bag, in the shared {@link Toolkit} shape.
     *
     * <p>
     * Its catch-all folds each tool id into the toolkit's tool map while the declared unlock and
     * in-use keys stay out of it. The wire may send no unlock key at all, in which case the bag
     * reads as locked however plainly it is in use.
     */
    @SerializedName("hunting_toolkit")
    private @NotNull Toolkit huntingToolkit = new Toolkit();

    // Tree Gifts

    /**
     * Tree gifts opened, keyed by tree species.
     *
     * <p>
     * The wire's {@code tree_gifts} is a flat object that also carries a nested
     * {@code milestone_tier_claimed} object, which cannot bind to the declared value type. Leniency
     * diverts that nested object to overflow, which is where {@code claimedMilestoneTiers} reads it
     * from; it is not here to tolerate unknown tree species.
     */
    @Lenient
    @SerializedName("tree_gifts")
    private @NotNull ConcurrentMap<String, Integer> treeGifts = Concurrent.newMap();

    /**
     * The highest tree gift milestone claimed, keyed by tree species.
     *
     * <p>
     * Lifted out of the sibling tree gift subtree, so it carries no root key of its own and a
     * re-serialize must not emit one.
     */
    @Extract("treeGifts.milestone_tier_claimed")
    private @NotNull ConcurrentMap<String, Integer> claimedMilestoneTiers = Concurrent.newMap();

    // Hina

    /**
     * Hina's task tracker, read from the nested {@code hina.tasks} node.
     */
    @SerializedPath("hina.tasks")
    private @NotNull Hina hina = new Hina();

    // Melody Harp

    /**
     * The member's harp record, read from the nested {@code songs.harp} node.
     */
    @SerializedPath("songs.harp")
    private @NotNull MelodyHarp melodyHarp = new MelodyHarp();

    /**
     * A member's progress through Hina's task list.
     *
     * <p>
     * Hina is the NPC in Tangleburg who introduces the Heart of the Forest, and her tasks form a
     * long checklist of Foraging, fishing and shard-fusion goals that pays out in tiers.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Hina">Hina</a>
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Hina {

        /**
         * Ids of the tasks the member has finished.
         */
        @SerializedName("completed_tasks")
        private @NotNull ConcurrentList<String> completedTasks = Concurrent.newList();

        /**
         * Running counters for the tasks that count something, keyed by the untiered stem of the
         * task id.
         *
         * <p>
         * A stem drops the threshold suffix that a completed task id carries, so this map and the
         * completed task ids do not share a key space.
         */
        @SerializedName("task_progress")
        private @NotNull ConcurrentMap<String, Integer> taskProgress = Concurrent.newMap();

        /**
         * Ids of the tasks whose reward has been collected.
         *
         * <p>
         * Not the completed task list by construction - a task can be finished with its reward left
         * unclaimed.
         */
        @SerializedName("claimed_rewards")
        private @NotNull ConcurrentList<String> claimedRewards = Concurrent.newList();

        /**
         * The highest task tier the member has claimed.
         */
        @SerializedName("tier_claimed")
        private int tierClaimed;

    }

}
