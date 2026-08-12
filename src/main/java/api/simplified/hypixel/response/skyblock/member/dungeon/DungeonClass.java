package api.simplified.hypixel.response.skyblock.member.dungeon;

import api.simplified.hypixel.common.EnumLookup;
import dev.simplified.util.StringUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
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
    @RequiredArgsConstructor
    public enum Type {

        /**
         * No class the wire named - the lookup fallback, and what a member who has never picked one
         * reads as.
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

        /**
         * Reads the class a wire name refers to.
         *
         * @param name the wire spelling of the class, matched case-insensitively
         * @return the matching class, or {@link #UNKNOWN} when no constant carries that name
         */
        public static @NotNull Type of(@NotNull String name) {
            return EnumLookup.of(values(), name, UNKNOWN);
        }

    }

}
