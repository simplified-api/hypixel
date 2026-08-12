package api.simplified.hypixel.response.skyblock.member.pet;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A member's autopet configuration - the rules that summon a pet automatically when something
 * happens.
 *
 * <p>
 * Rule slots are bought with bits, two at a time, up to 28 rules, and one rule can carry up to five
 * exceptions that suppress it. A member who never configured autopet sends an empty rule list and
 * none of the other keys.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Autopet">Autopet</a>
 */
@Getter
public class AutoPet {

    /**
     * Rule slots bought, up to 28. This is the purchased cap rather than the number of rules
     * configured.
     */
    @SerializedName("rules_limit")
    private int rulesLimit;

    /**
     * The configured rules.
     */
    private @NotNull ConcurrentList<Rule> rules = Concurrent.newList();

    /**
     * Hypixel's own flag marking this node as migrated from an older autopet format - upstream
     * schema bookkeeping rather than member state, and nothing should branch on it.
     */
    private boolean migrated;

    /**
     * A second upstream migration flag, with the same meaning and the same caveat as the first.
     */
    @SerializedName("migrated_2")
    private boolean migrated2;

    /**
     * One autopet rule - a trigger, the pet it summons, and the conditions that suppress it.
     *
     * <p>
     * Triggers cover logging in, gaining skill experience, entering an island, entering combat,
     * gaining a collection, killing a mob, picking a dungeon class, an event starting, a boss
     * spawning, starting a slayer quest, using the experimentation table, going AFK, digging a
     * griffin burrow, opening a winter gift, casting a fishing rod, and entering a given Catacombs
     * floor.
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Rule {

        /**
         * The rule's own id, which the wire sends as 32 hex digits with no hyphens.
         *
         * <p>
         * The hyphens are inserted while binding, and the write half always emits the canonical
         * dashed form, so this id does not round-trip to the spelling it arrived in.
         */
        @SerializedName("uuid")
        private UUID identifier;

        /**
         * The trigger that fires the rule, such as {@code FISH}, {@code DIG_BURROW} or
         * {@code ENTER_ISLAND}.
         */
        private String id;

        /**
         * Hypixel's rendered label for the rule, carrying section colour codes and symbols. It is a
         * display string, not an identifier - the pet the rule summons is named by its unique id.
         */
        private String name;

        /**
         * The unique id of the pet this rule summons, which the wire spells {@code uniqueId} and
         * sends in the canonical dashed form that the write half emits, unlike the rule's own id.
         *
         * <p>
         * The reference may dangle: a rule outlives the pet it names, and two rules may point at the
         * same pet.
         */
        @SerializedName("uniqueId")
        private UUID petUniqueId;

        /**
         * Whether the rule is switched off.
         */
        private boolean disabled;

        /**
         * Conditions that stop the rule firing, up to five of them.
         */
        private @NotNull ConcurrentList<Exception> exceptions = Concurrent.newList();

        /**
         * The trigger's parameters as string pairs - empty for most triggers, and carrying the
         * island id under {@code island} for an island trigger. It is a parameter bag, not free-form
         * metadata.
         */
        private @NotNull ConcurrentMap<String, String> data = Concurrent.newMap();

        /**
         * One condition that stops an autopet rule firing.
         *
         * <p>
         * The available kinds cover being on or off a given island, already having a particular pet
         * equipped, an event being active, a slayer quest being active, a given day of the week, and
         * a slayer boss being spawned.
         *
         * <p>
         * Despite the name this is not a {@link Throwable}. Inside {@link Rule} the simple name
         * resolves here and shadows the one in {@code java.lang}.
         */
        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Exception {

            /**
             * The kind of condition, such as {@code HAS_EQUIPPED_PET}. The vocabulary is the
             * condition's, and does not overlap the trigger ids a rule carries.
             */
            private String id;

            /**
             * The condition's parameters as string pairs, such as the pet type id under
             * {@code pet}.
             */
            private @NotNull ConcurrentMap<String, String> data = Concurrent.newMap();

        }

    }

}
