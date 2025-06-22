package dev.sbs.minecraftapi.client.hypixel.response.hypixel.implementation;

import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.text.ChatFormat;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
public class HypixelRank {

    private final @NotNull Type type;
    private final @NotNull ChatFormat rankFormat;
    private final @NotNull ChatFormat plusFormat;
    private final @NotNull String pluses;

    public HypixelRank(@NotNull Type type, @NotNull ChatFormat rankFormat, @NotNull ChatFormat plusFormat) {
        this.type = type;
        this.rankFormat = rankFormat;
        this.plusFormat = plusFormat;
        this.pluses = StringUtil.repeat('+', this.getType().getPlusCount());
    }

    @Override
    public String toString() {
        String sfPluses = String.format("%s%s", this.getPlusFormat(), this.getPluses());
        String sfRank = String.format("%s%s", this.getRankFormat(), this.getType().getName());
        return String.format("%s[%s%s%s]", ChatFormat.WHITE, sfRank, sfPluses, ChatFormat.WHITE);
    }

    @Getter
    public enum Type {

        OWNER(ChatFormat.RED),
        ADMIN(ChatFormat.RED),
        GAME_MASTER(ChatFormat.DARK_GREEN),
        YOUTUBER(ChatFormat.RED, "YOUTUBE"),
        SUPERSTAR(ChatFormat.GOLD, "MVP", 2),
        MVP_PLUS(ChatFormat.AQUA, "MVP", 1),
        MVP(ChatFormat.AQUA),
        VIP_PLUS(ChatFormat.GREEN, "VIP", 1),
        VIP(ChatFormat.GREEN),
        PIG(ChatFormat.LIGHT_PURPLE, "PIG", 3),
        NONE(ChatFormat.GRAY);

        private final @NotNull ChatFormat format;
        private final @NotNull String name;
        private final int plusCount;

        Type(@NotNull ChatFormat format) {
            this(format, null);
        }

        Type(@NotNull ChatFormat format, String name) {
            this(format, name, 0);
        }

        Type(@NotNull ChatFormat format, String name, int plusCount) {
            this.format = format;
            this.name = (StringUtil.isEmpty(name) ? name() : name).replace("_", " ");
            this.plusCount = plusCount;
        }

        public static @NotNull Type of(@NotNull String name) {
            //name = name.replaceAll("\\+", "");
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(NONE);
        }

    }

}