package api.simplified.hypixel.response.skyblock.member.dungeon;

import dev.simplified.util.StringUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public class DungeonData implements DungeonWeighted {

    private final @NotNull FloorData normalMode;
    private final @NotNull FloorData masterMode;

    /** {@inheritDoc} */
    @Override
    public double getExperience() {
        return this.getNormalMode().getExperience();
    }


    public @NotNull FloorData getFloorData(boolean masterMode) {
        return masterMode ? this.getMasterMode() : this.getNormalMode();
    }



    @Getter
    @RequiredArgsConstructor
    public enum Type {

        UNKNOWN,
        CATACOMBS;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

        public static @NotNull Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }


}
