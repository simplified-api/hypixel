package api.simplified.hypixel.response.skyblock.member.dungeon;

import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.AllArgsConstructor;
import dev.simplified.annotations.EnumLookup;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.util.StringUtil;
import org.jetbrains.annotations.NotNull;


/**
 * A member's mastery of one dungeon class.
 * <p>
 * Dungeoneering is made of Catacombs levels plus a separate mastery per class, each earning its own
 * experience from the runs played as that class. One field is the whole class, because the wire node
 * already is that shape - there is no richer object being reduced to its experience.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Classes">Classes</a>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DungeonClass implements DungeonWeighted {

    /**
     * Class experience earned playing that class.
     */
    private double experience;




    /**
     * The five roles a player can take into a dungeon, each with its own passives, Dungeon Orb
     * abilities and ghost abilities.
     * <p>
     * A party may run duplicates, but a class only one player has picked gets doubled ability
     * strength. The wire spells these lowercase everywhere and the lookup is case-insensitive, so
     * every spelling binds - but serialization writes the constant name, so a round trip turns
     * {@code healer} into {@code HEALER}. Case does not survive it.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Classes">Classes</a>
     */
    @EnumLookup
    public enum Type {

        /**
         * No class the wire named - the sentinel an unmatched name resolves to, and what a member who
         * has never picked one reads as.
         */
        UNKNOWN,

        /**
         * Support built around healing teammates; its milestone counts health healed.
         */
        HEALER,

        /**
         * Magic and area-of-effect damage, strong on waves and weak on tanky single targets.
         */
        MAGE,

        /**
         * Melee single-target damage with lifesteal, the strongest sustained damage late game.
         */
        BERSERK,

        /**
         * Ranged bow damage, extremely fragile, scaling on crit damage and strength.
         */
        ARCHER,

        /**
         * Effective health and pulling mobs off teammates; its milestone counts damage tanked and
         * dealt.
         */
        TANK;

        /**
         * Title-cased constant name, {@code BERSERK} reading as {@code Berserk}.
         */
        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name());
        }

    }

}
