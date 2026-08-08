package api.simplified.hypixel.response.skyblock.member.dungeon;

import dev.simplified.util.StringUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DungeonClass implements DungeonWeighted {

    private double experience;




    @RequiredArgsConstructor
    public enum Type {

        UNKNOWN,
        HEALER,
        MAGE,
        BERSERK,
        ARCHER,
        TANK;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name());
        }

        public static @NotNull Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

}
