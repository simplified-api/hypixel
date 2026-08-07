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

@Getter
public class Dojo {

    @Capture(filter = "^dojo_points_")
    private @NotNull ConcurrentMap<Type, Integer> points = Concurrent.newMap();
    @Capture(filter = "^dojo_time_")
    private @NotNull ConcurrentMap<Type, Integer> times = Concurrent.newMap();
    // the keys no Type constant matched, under the original unstripped spelling
    @Extract("points")
    private @NotNull ConcurrentMap<String, Integer> unknownPoints = Concurrent.newMap();
    @Extract("times")
    private @NotNull ConcurrentMap<String, Integer> unknownTimes = Concurrent.newMap();

    @Getter
    @RequiredArgsConstructor
    public enum Type {

        UNKNOWN(""),
        // the wire name has to be a @SerializedName as well as a constructor component: of() reads
        // the component, but the enum adapter that binds these as @Capture map keys only ever sees
        // names, so without this every dojo entry misses and is diverted as unmatched
        @SerializedName("mob_kb")
        FORCE("mob_kb"),
        @SerializedName("wall_jump")
        STAMINA("wall_jump"),
        @SerializedName("archer")
        MASTERY("archer"),
        @SerializedName("sword_swap")
        DISCIPLINE("sword_swap"),
        @SerializedName("snake")
        SWIFTNESS("snake"),
        @SerializedName("fireball")
        CONTROL("fireball"),
        @SerializedName("lock_head")
        TENACITY("lock_head");

        private final @NotNull String internalName;

        public static @NotNull Dojo.Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name) || type.getInternalName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

}
