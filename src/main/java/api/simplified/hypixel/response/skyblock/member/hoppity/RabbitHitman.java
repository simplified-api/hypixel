package api.simplified.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;

import java.time.Instant;

/**
 * The chocolate eggs Hoppity's hitman is holding for a member, and the slots they sit in.
 *
 * <p>
 * The hitman recovers eggs the member never collected and stores one per slot until it is claimed.
 * Claiming puts that slot on a cooldown, and only one slot's cooldown runs at a time, so a queue of
 * slots frees up one after another. Slots are bought with coins, up to 28 of them, and some are
 * gated behind factory levels.
 *
 * <p>
 * The whole node is absent for a member who never unlocked a slot. The wire names it in the plural
 * even though this is one hitman.
 */
@Getter
public class RabbitHitman {

    /**
     * Egg storage slots unlocked, up to 28.
     */
    @SerializedName("rabbit_hitmen_slots")
    private int slots;

    /**
     * Eggs the hitman is holding that the member has not claimed yet. Zero means nothing is waiting
     * to be claimed, not that no egg was ever missed.
     */
    @SerializedName("missed_uncollected_eggs")
    private int missedUncollectedEggs;

    /**
     * The mark the sequential slot cooldown is measured from. It is a starting point rather than a
     * deadline - the queue drains at this instant plus the queued cooldown total.
     */
    @SerializedName("egg_slot_cooldown_mark")
    private Instant eggSlotCooldown;

    /**
     * Total queued cooldown in milliseconds across every slot still recovering.
     */
    @SerializedName("egg_slot_cooldown_sum")
    private int eggSlotCooldownSum;

    /**
     * When the hitman last recovered an egg for the member.
     */
    @SerializedName("egg_finder_last_found")
    private Instant eggFinderLastFound;

}
