package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.dungeon;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.util.StringUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public enum Floor {

    @SerializedName("0")
    ENTRANCE(0, Size.TINY, Boss.WATCHER),
    @SerializedName("1")
    ONE(1, Size.TINY, Boss.BONZO),
    @SerializedName("2")
    TWO(2, Size.SMALL, Boss.SCARF),
    @SerializedName("3")
    THREE(3, Size.SMALL, Boss.THE_PROFESSOR),
    @SerializedName("4")
    FOUR(4, Size.SMALL, Boss.THORN),
    @SerializedName("5")
    FIVE(5, Size.MEDIUM, Boss.LIVID),
    @SerializedName("6")
    SIX(6, Size.MEDIUM, Boss.SADAN),
    @SerializedName("7")
    SEVEN(7, Size.LARGE, Boss.NECRON);

    private final int value;
    private final @NotNull Size size;
    private final @NotNull Boss boss;

    public static @NotNull Floor of(int value) {
        for (Floor floor : values()) {
            if (floor.getValue() == value)
                return floor;
        }

        return ENTRANCE;
    }

    public enum Boss {

        WATCHER,
        BONZO,
        SCARF,
        THE_PROFESSOR,
        THORN,
        LIVID,
        SADAN,
        NECRON;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

    }

    public enum Size {

        TINY,
        SMALL,
        MEDIUM,
        LARGE;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name());
        }

    }

}
