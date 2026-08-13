package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.SkyBlockIsland;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import api.simplified.hypixel.response.skyblock.member.AccessoryBag;
import api.simplified.skyblock.model.BonusPetPerkStat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Everything one compute reads: the member being totalled, the island they are on, and the variables
 * a bonus expression can name.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class StatContext {

    /**
     * The profile the member belongs to, read for the shared bank balance.
     */
    private final @NotNull SkyBlockIsland island;

    /**
     * The member being totalled.
     */
    private final @NotNull SkyBlockMember member;

    /**
     * The member's accessory bag, with its accessories already resolved.
     */
    private final @NotNull AccessoryBag accessoryBag;

    /**
     * The player state a bonus expression can refer to.
     */
    private final @NotNull ConcurrentMap<String, Double> variables = Concurrent.newMap();

    /**
     * Conditional bonuses declared for the active pet's perks, gathered as the pet is read.
     */
    private final @NotNull ConcurrentList<BonusPetPerkStat> bonusPetPerkStats = Concurrent.newList();

    /**
     * Republishes every stat total under {@code STAT_<statId>}, so the next pass's expressions read
     * the pass that has just finished rather than one that is still running.
     *
     * @param table the table to read the totals from
     */
    public void publishTotals(@NotNull StatTable table) {
        table.toMap().forEach((statModel, data) -> this.variables.put(
            String.format("STAT_%s", statModel.getId()),
            data.getTotal()
        ));
    }

}
