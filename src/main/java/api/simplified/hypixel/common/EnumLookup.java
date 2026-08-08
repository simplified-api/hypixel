package api.simplified.hypixel.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Case-insensitive lookup of an enum constant by name, falling back to a sentinel.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EnumLookup {

    /**
     * Finds the constant whose name matches, ignoring case.
     *
     * @param values the constants to search, normally {@code values()}
     * @param name the wire name to match
     * @param fallback the constant to return when nothing matches
     * @param <E> the enum type
     * @return the matching constant, or {@code fallback}
     */
    public static <E extends Enum<E>> @NotNull E of(E @NotNull [] values, @NotNull String name, @NotNull E fallback) {
        return Arrays.stream(values)
            .filter(value -> value.name().equalsIgnoreCase(name))
            .findFirst()
            .orElse(fallback);
    }

}
