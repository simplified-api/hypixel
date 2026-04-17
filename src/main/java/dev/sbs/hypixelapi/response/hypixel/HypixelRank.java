package dev.sbs.minecraftapi.client.hypixel.response.hypixel;

import dev.sbs.renderer.text.ChatColor;
import dev.simplified.util.StringUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
public class HypixelRank {

    private final @NotNull Type type;
    private final @NotNull ChatColor rankFormat;
    private final @NotNull ChatColor plusFormat;
    private final @NotNull String pluses;

    public HypixelRank(@NotNull Type type, @NotNull ChatColor rankFormat, @NotNull ChatColor plusFormat) {
        this.type = type;
        this.rankFormat = rankFormat;
        this.plusFormat = plusFormat;
        this.pluses = StringUtil.repeat('+', this.getType().getPlusCount());
    }

    @Override
    public String toString() {
        String sfPluses = String.format("%s%s", this.getPlusFormat(), this.getPluses());
        String sfRank = String.format("%s%s", this.getRankFormat(), this.getType().getName());
        return String.format("%s[%s%s%s]", ChatColor.Legacy.WHITE, sfRank, sfPluses, ChatColor.Legacy.WHITE);
    }

    @Getter
    public enum Type {

        OWNER(ChatColor.Legacy.RED),
        ADMIN(ChatColor.Legacy.RED),
        GAME_MASTER(ChatColor.Legacy.DARK_GREEN),
        YOUTUBER(ChatColor.Legacy.RED, "YOUTUBE"),
        SUPERSTAR(ChatColor.Legacy.GOLD, "MVP", 2),
        MVP_PLUS(ChatColor.Legacy.AQUA, "MVP", 1),
        MVP(ChatColor.Legacy.AQUA),
        VIP_PLUS(ChatColor.Legacy.GREEN, "VIP", 1),
        VIP(ChatColor.Legacy.GREEN),
        PIG(ChatColor.Legacy.LIGHT_PURPLE, "PIG", 3),
        INNIT(ChatColor.Legacy.LIGHT_PURPLE, "INNIT", 0),
        NONE(ChatColor.Legacy.GRAY);

        private final @NotNull ChatColor color;
        private final @NotNull String name;
        private final int plusCount;

        Type(@NotNull ChatColor.Legacy color) {
            this(color, null);
        }

        Type(@NotNull ChatColor.Legacy color, String name) {
            this(color, name, 0);
        }

        Type(@NotNull ChatColor.Legacy color, String name, int plusCount) {
            this.color = color;
            this.name = (StringUtil.isEmpty(name) ? name() : name).replace("_", " ");
            this.plusCount = plusCount;
        }

        public static @NotNull Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(NONE);
        }

    }

}