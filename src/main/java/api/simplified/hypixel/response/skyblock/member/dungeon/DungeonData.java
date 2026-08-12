package api.simplified.hypixel.response.skyblock.member.dungeon;

import api.simplified.hypixel.common.EnumLookup;
import dev.simplified.util.StringUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;


/**
 * One dungeon with both of its difficulties held together.
 * <p>
 * Master mode is not a separate dungeon: it is the same floors with the same bosses, harder mobs,
 * tighter score timers and its own completion counters. Instances are built by {@link Dungeons} out
 * of the raw floor records and are never bound by gson, which is why neither field carries a wire
 * annotation.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Catacombs">Catacombs</a>
 */
@Getter
@RequiredArgsConstructor
public class DungeonData implements DungeonWeighted {

    /**
     * The normal-difficulty record for this dungeon.
     */
    private final @NotNull FloorData normalMode;

    /**
     * The master mode record for this dungeon, an empty record when the wire sent no master node.
     */
    private final @NotNull FloorData masterMode;

    /**
     * {@inheritDoc}
     * <p>
     * Read from the normal difficulty. Master mode grants Catacombs experience into the same pool,
     * so the master node carries no experience key of its own and reading that side would always
     * give zero.
     */
    @Override
    public double getExperience() {
        return this.getNormalMode().getExperience();
    }


    /**
     * Reads one side of the difficulty pair.
     *
     * @param masterMode whether to read the master mode side
     * @return the floor record, empty rather than null for a dungeon the wire sent no master node
     * for
     */
    public @NotNull FloorData getFloorData(boolean masterMode) {
        return masterMode ? this.getMasterMode() : this.getNormalMode();
    }



    /**
     * Which dungeon a record belongs to.
     * <p>
     * The Catacombs is the only dungeon released; two more have been teased in the Dungeon Hub and
     * the Dungeoneering menu, where they are listed as "Not Coming Soon".
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Dungeons">Dungeons</a>
     */
    @Getter
    @RequiredArgsConstructor
    public enum Type {

        /**
         * No dungeon this table names - the lookup fallback, and the key an empty record answers
         * to.
         */
        UNKNOWN,

        /**
         * The Catacombs, the only released dungeon, entered through Mort in the Dungeon Hub or at
         * the Catacombs Entrance.
         */
        CATACOMBS;

        /**
         * Title-cased constant name with underscores as spaces, {@code CATACOMBS} reading as
         * {@code Catacombs}.
         */
        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

        /**
         * Reads the dungeon a wire name refers to.
         * <p>
         * The vocabulary is {@code CATACOMBS} and nothing else - {@code master_catacombs} is not a
         * member of it. The master prefix is stripped before the lookup runs, which is the only
         * reason a master node pairs onto its normal floor instead of resolving here.
         *
         * @param name the wire spelling of the dungeon, matched case-insensitively
         * @return the matching dungeon, or {@link #UNKNOWN} when no constant carries that name
         */
        public static @NotNull Type of(@NotNull String name) {
            return EnumLookup.of(values(), name, UNKNOWN);
        }

    }


}
