package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.stats.buff.BuffEvaluator;
import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.Buff;
import org.jetbrains.annotations.NotNull;

/**
 * What a compute can know about the world it runs in, resolved once and written onto every context a
 * row is folded against.
 *
 * <p>
 * It is a value rather than a filled context because a context is mutable and carries per-carrier
 * readings, so one shared between slots would leak a rarity from the last slot into the next. These
 * four are the same for the whole compute, so they resolve once and are applied to each fresh one.
 *
 * <p>
 * Four of the nine properties a {@link Buff.Term.Kind#WORLD} term can name are answerable and five
 * are not, and the split is a property of the input rather than of work left to do. A profiles
 * response says what a member <b>has</b>; it carries nothing about where they are standing or what
 * they are hitting. A term naming one of the five resolves empty, which leaves the rule reading it
 * folding nothing rather than folding a wrong number.
 *
 * <table>
 * <caption>Where each property does or does not come from</caption>
 * <tr><th>Property</th><th>Source</th></tr>
 * <tr><td>{@code MODE}</td><td>the island's game mode</td></tr>
 * <tr><td>{@code DUNGEON_CLASS}</td><td>the class the member last queued as</td></tr>
 * <tr><td>{@code SEASON}, {@code HOUR}</td><td>the SkyBlock calendar, read now</td></tr>
 * <tr><td>{@code REGION}, {@code ZONE}</td><td><b>nothing</b> - a profile carries no position</td></tr>
 * <tr><td>{@code MOB_TYPE}</td><td><b>nothing</b> - what is being fought is not a profile fact</td></tr>
 * <tr><td>{@code MAYOR}</td><td><b>nothing here</b> - it is a separate election read, and a compute takes no network</td></tr>
 * <tr><td>{@code EVENT}</td><td><b>nothing</b> - no calendar maps a date onto an event row</td></tr>
 * </table>
 *
 * <p>
 * <b>The two calendar readings make a total depend on when it is taken</b>, which is the one thing a
 * characterisation baseline cannot survive. That is why the harness asserts no shipped row gates on
 * either: the capability is here so that a row needing it is not silently inert, and the corpus is
 * what is held still.
 *
 * <p>
 * An item's rarity resolves during its own construction, before any of this exists, so a rarity rule
 * gating on the world is inert whatever this holds.
 *
 * @param mode the island's game mode
 * @param dungeonClass the class the member last queued as
 * @param season the SkyBlock season as of now
 * @param hour the hour of the SkyBlock day as of now
 */
record WorldFacts(@NotNull String mode, @NotNull String dungeonClass, @NotNull String season, int hour) {

    /**
     * The answer for a caller that has no member to read - a test driving the pass directly. Every
     * property resolves empty, which is the same state the five unreachable ones are always in.
     */
    static final @NotNull WorldFacts NONE = new WorldFacts("", "", "", -1);

    /**
     * Resolves what this compute can know.
     *
     * @param statContext the member and the island to read from
     * @return the facts, read once
     */
    static @NotNull WorldFacts of(@NotNull StatContext statContext) {
        SkyBlockDate now = new SkyBlockDate(System.currentTimeMillis());

        return new WorldFacts(
            statContext.getIsland().getGameMode().name(),
            statContext.getMember().getDungeons().getSelectedClass().name(),
            now.getSeason().name(),
            now.getHour()
        );
    }

    /**
     * Writes these onto one context.
     *
     * <p>
     * An empty reading is left unwritten rather than written empty, so a term naming it stays
     * unanswered - the state that leaves a rule inert instead of matching the empty string.
     *
     * @param context the context to fill
     * @return the same context, filled
     */
    @NotNull BuffEvaluator.Context fill(@NotNull BuffEvaluator.Context context) {
        if (!this.mode().isEmpty())
            context.world(Buff.Term.World.MODE, this.mode());

        if (!this.dungeonClass().isEmpty())
            context.world(Buff.Term.World.DUNGEON_CLASS, this.dungeonClass());

        if (!this.season().isEmpty())
            context.world(Buff.Term.World.SEASON, this.season());

        if (this.hour() >= 0)
            context.world(Buff.Term.World.HOUR, this.hour());

        return context;
    }

}
