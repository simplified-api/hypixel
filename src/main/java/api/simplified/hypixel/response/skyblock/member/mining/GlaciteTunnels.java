package api.simplified.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

/**
 * A member's progress in the Glacite Tunnels, the mining location that opens at Heart of the
 * Mountain VII.
 *
 * <p>
 * It records fossil excavation - which fossils have been donated and how much fossil dust is banked
 * - alongside the Glacite Mineshafts entered from the tunnels and the frozen corpses looted inside
 * them. Everything here is bound; nothing is derived and nothing reaches a repository.
 *
 * <p>
 * Bound from {@code glacite_player_data}, a node the wire omits entirely for a member who has never
 * been down there. Absence leaves every field on its non-null default and is the ordinary case, not
 * an error.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Glacite_Tunnels">Glacite Tunnels</a>
 */
@Getter
public class GlaciteTunnels {

    /**
     * Ids of the fossils already handed to the Fossil Excavator, one entry per distinct fossil.
     *
     * <p>
     * The ids are bare and untiered, and not every one of them is an excavation drop - some are
     * Crystal Nucleus rewards - so this is what has been donated rather than what was dug up.
     */
    @SerializedName("fossils_donated")
    private @NotNull ConcurrentList<String> donatedFossils = Concurrent.newList();

    /**
     * The fossil dust balance spent on excavation.
     *
     * <p>
     * A fractional balance rather than a whole-number count; it arrives carrying the floating-point
     * noise of everything that was added to it.
     */
    @SerializedName("fossil_dust")
    private double fossilDust;

    /**
     * Lifetime frozen corpses opened, keyed by the corpse type.
     *
     * <p>
     * The wire spells its keys in lowercase and the shared case-insensitive lookup matches them onto
     * {@link CorpseType}; a case-sensitive lookup would drop every entry.
     */
    @SerializedName("corpses_looted")
    private @NotNull ConcurrentMap<CorpseType, Integer> lootedCorpses = Concurrent.newMap();

    /**
     * Lifetime entries into a Glacite Mineshaft.
     */
    @SerializedName("mineshafts_entered")
    private int enteredMineshafts;

    /**
     * The four frozen corpses found in Glacite Mineshafts.
     *
     * <p>
     * Each is opened with the matching key and drops loot themed to its mineshaft. No constant
     * declares a wire name; the wire sends them lowercase and the shared lookup matches them
     * case-insensitively.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Frozen_Corpses">Frozen Corpses</a>
     */
    public enum CorpseType {

        /**
         * The common lapis corpse.
         */
        LAPIS,

        /**
         * The tungsten corpse.
         */
        TUNGSTEN,

        /**
         * The umber corpse.
         */
        UMBER,

        /**
         * The Vanguard, the rarest of the four.
         */
        VANGUARD

    }

}
