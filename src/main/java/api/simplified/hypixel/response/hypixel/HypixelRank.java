package api.simplified.hypixel.response.hypixel;

import dev.simplified.annotations.EnumLookup;
import dev.simplified.annotations.Getter;
import dev.simplified.util.StringUtil;
import lib.minecraft.text.ChatColor;
import org.jetbrains.annotations.NotNull;

/**
 * A rank a player holds on the Hypixel network, together with the two colours it renders in - one
 * for the name and one for the plus signs, which a player can set independently.
 */
@Getter
public class HypixelRank {

    /**
     * The rank held.
     */
    private final @NotNull Type type;

    /**
     * {@link ChatColor} the rank name renders in.
     */
    private final @NotNull ChatColor rankFormat;

    /**
     * {@link ChatColor} the trailing plus signs render in.
     */
    private final @NotNull ChatColor plusFormat;

    /**
     * The plus signs trailing the rank name, one per plus the rank carries.
     */
    private final @NotNull String pluses;

    /**
     * Constructs a rank from its type and the colours it renders in.
     *
     * @param type the rank held
     * @param rankFormat the colour the rank name renders in
     * @param plusFormat the colour the trailing plus signs render in
     */
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

    /**
     * Every rank the network hands out, in descending order of standing - staff first, then the
     * purchasable ranks, then the one-off ranks, then no rank at all.
     */
    @Getter
    @EnumLookup
    public enum Type {

        /**
         * The network owner, rendered red.
         */
        OWNER(ChatColor.Legacy.RED),

        /**
         * A Hypixel administrator, rendered red.
         */
        ADMIN(ChatColor.Legacy.RED),

        /**
         * A Hypixel game master, rendered dark green as {@code GAME MASTER}.
         */
        GAME_MASTER(ChatColor.Legacy.DARK_GREEN),

        /**
         * A partnered content creator, rendered red as {@code YOUTUBE}.
         */
        YOUTUBER(ChatColor.Legacy.RED, "YOUTUBE"),

        /**
         * MVP++, the monthly subscription rank, rendered as {@code MVP} with two pluses. Gold is only
         * its default - the player picks the name colour, and it is the one rank whose name colour is
         * read off the wire.
         */
        SUPERSTAR(ChatColor.Legacy.GOLD, "MVP", 2),

        /**
         * MVP+, rendered aqua as {@code MVP} with one plus.
         */
        MVP_PLUS(ChatColor.Legacy.AQUA, "MVP", 1),

        /**
         * MVP, rendered aqua with no pluses.
         */
        MVP(ChatColor.Legacy.AQUA),

        /**
         * VIP+, rendered green as {@code VIP} with one plus.
         */
        VIP_PLUS(ChatColor.Legacy.GREEN, "VIP", 1),

        /**
         * VIP, rendered green with no pluses.
         */
        VIP(ChatColor.Legacy.GREEN),

        /**
         * PIG+++, a one-off rank rendered light purple with three pluses that always render aqua.
         */
        PIG(ChatColor.Legacy.LIGHT_PURPLE, "PIG", 3),

        /**
         * INNIT, a one-off rank rendered light purple with no pluses.
         */
        INNIT(ChatColor.Legacy.LIGHT_PURPLE, "INNIT", 0),

        /**
         * No rank, rendered gray, and what an unrecognised rank name falls back to.
         */
        NONE(ChatColor.Legacy.GRAY);

        /**
         * Colour the rank name renders in unless the player has overridden it.
         */
        private final @NotNull ChatColor color;

        /**
         * Name the rank renders under, which is the constant's own name with underscores turned into
         * spaces unless an override was given.
         */
        private final @NotNull String name;

        /**
         * How many plus signs trail the rank name.
         */
        private final int plusCount;

        /**
         * Constructs a rank type rendering under its own name with no pluses.
         *
         * @param color the colour the rank name renders in
         */
        Type(@NotNull ChatColor.Legacy color) {
            this(color, null);
        }

        /**
         * Constructs a rank type rendering under an overridden name with no pluses.
         *
         * @param color the colour the rank name renders in
         * @param name the name to render under, or empty to use the constant's own name
         */
        Type(@NotNull ChatColor.Legacy color, String name) {
            this(color, name, 0);
        }

        /**
         * Constructs a rank type rendering under an overridden name with trailing pluses.
         *
         * @param color the colour the rank name renders in
         * @param name the name to render under, or empty to use the constant's own name
         * @param plusCount how many plus signs trail the rank name
         */
        Type(@NotNull ChatColor.Legacy color, String name, int plusCount) {
            this.color = color;
            this.name = (StringUtil.isEmpty(name) ? name() : name).replace("_", " ");
            this.plusCount = plusCount;
        }

    }

}