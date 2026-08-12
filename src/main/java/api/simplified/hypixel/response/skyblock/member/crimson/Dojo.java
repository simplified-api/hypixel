package api.simplified.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import dev.simplified.gson.annotation.Extract;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * A member's scores across Master Tao's seven tests.
 * <p>
 * Points accumulate per test and the sum across all seven is what buys belts. Points and times arrive
 * as two flat key spaces, {@code dojo_points_*} and {@code dojo_time_*}, and the two {@link Capture}
 * filters are disjoint so both can live on one class; each strips its own prefix, so both map onto the
 * same {@link Type} key space.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Dojo">Dojo</a>
 */
@Getter
public class Dojo {

    /**
     * Points scored per test, gathered from every {@code dojo_points_} key with the prefix stripped.
     */
    @Capture(filter = "^dojo_points_")
    private @NotNull ConcurrentMap<Type, Integer> points = Concurrent.newMap();

    /**
     * Time recorded per test in milliseconds - not ticks and not seconds - gathered the same way from
     * every {@code dojo_time_} key.
     */
    @Capture(filter = "^dojo_time_")
    private @NotNull ConcurrentMap<Type, Integer> times = Concurrent.newMap();

    /**
     * Point keys whose remainder is not a known test, kept under the spelling the wire gave them.
     * Without the diversion every unmatched key would collapse onto one {@code null} map entry and all
     * but the last would be lost.
     */
    @Extract("points")
    private @NotNull ConcurrentMap<String, Integer> unknownPoints = Concurrent.newMap();

    /**
     * Time keys whose remainder is not a known test, diverted the same way as the unmatched points.
     */
    @Extract("times")
    private @NotNull ConcurrentMap<String, Integer> unknownTimes = Concurrent.newMap();

    /**
     * The seven tests Master Tao offers, each with its own arena, its own objective and its own point
     * and time total.
     * <p>
     * Every real test carries its wire name twice, once as a {@link SerializedName} and once as a
     * constructor component, and both are needed: {@link #of(String)} reads the component, while the
     * enum adapter that binds these as map keys only ever sees names.
     *
     * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Dojo">Dojo</a>
     */
    @Getter
    @RequiredArgsConstructor
    public enum Type {

        /**
         * Not a test - the value {@link #of(String)} falls through to when a name matches nothing. Its
         * wire name is the empty string, so nothing on the wire reaches it directly.
         */
        UNKNOWN(""),
        // the wire name has to be a @SerializedName as well as a constructor component: of() reads
        // the component, but the enum adapter that binds these as @Capture map keys only ever sees
        // names, so without this every dojo entry misses and is diverted as unmatched

        /**
         * The Test of Force, which asks the member to knock mobs into the lava - some of them worth
         * more than others and some worth negative points.
         */
        @SerializedName("mob_kb")
        FORCE("mob_kb"),

        /**
         * The Test of Stamina, which asks the member to jump through the holes in the moving walls
         * without falling in the lava.
         */
        @SerializedName("wall_jump")
        STAMINA("wall_jump"),

        /**
         * The Test of Mastery, which asks the member to shoot targets before they disappear, each
         * colour of target scoring differently.
         */
        @SerializedName("archer")
        MASTERY("archer"),

        /**
         * The Test of Discipline, which asks the member to hit each mob with the sword matching its
         * helmet, the pairing changing as the run goes on.
         */
        @SerializedName("sword_swap")
        DISCIPLINE("sword_swap"),

        /**
         * The Test of Swiftness, which asks the member to cross temporary blocks laid over the lava
         * before they vanish.
         */
        @SerializedName("snake")
        SWIFTNESS("snake"),

        /**
         * The test bound from the wire name {@code fireball}. Which of Master Tao's challenges that
         * wire name belongs to is unresolved, so this constant is named and nothing more - repairing
         * the pairing would silently change what every past decode means.
         */
        @SerializedName("fireball")
        CONTROL("fireball"),

        /**
         * The test bound from the wire name {@code lock_head}, unresolved against a named challenge for
         * the same reason and left undescribed for the same reason.
         */
        @SerializedName("lock_head")
        TENACITY("lock_head");

        /**
         * The name the wire spells this test with, matched case-insensitively alongside the constant's
         * own name.
         */
        private final @NotNull String internalName;

        /**
         * Looks a test up by either the constant's name or the wire's, ignoring case.
         *
         * @param name the name to match
         * @return the matching test, or {@link #UNKNOWN} when neither name matches
         */
        public static @NotNull Dojo.Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name) || type.getInternalName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

}
