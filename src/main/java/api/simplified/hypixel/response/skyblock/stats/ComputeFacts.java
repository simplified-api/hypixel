package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.stats.buff.BuffEvaluator;
import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.Buff;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
 * <p>
 * The group counts sit here for the same reason: a group is declared in place, on the row that owns
 * it, so how many of a named set the member is wearing is one question answered once against the
 * filled sheet rather than per row per slot.
 *
 * @param mode the island's game mode
 * @param dungeonClass the class the member last queued as
 * @param season the SkyBlock season as of now
 * @param hour the hour of the SkyBlock day as of now
 * @param groupCounts how many members of each declared group the member is wearing, keyed by group id
 */
record ComputeFacts(
    @NotNull String mode,
    @NotNull String dungeonClass,
    @NotNull String season,
    int hour,
    @NotNull ConcurrentMap<String, Integer> groupCounts
) {

    /**
     * The answer for a caller that has no member to read - a test driving the pass directly. Every
     * property resolves empty, which is the same state the five unreachable ones are always in.
     */
    static final @NotNull ComputeFacts NONE = new ComputeFacts("", "", "", -1, Concurrent.newUnmodifiableMap());

    /**
     * Resolves what a caller running before the sheet is filled can know.
     *
     * <p>
     * The flat pass is that caller. It runs before any slot is open, so no group can be counted -
     * and a group gate belongs to the bonus pass anyway, which is where a set bonus reads what the
     * whole set came to.
     *
     * @param statContext the member and the island to read from
     * @return the facts, with no group counted
     */
    static @NotNull ComputeFacts of(@NotNull StatContext statContext) {
        SkyBlockDate now = new SkyBlockDate(System.currentTimeMillis());

        return new ComputeFacts(
            statContext.getIsland().getGameMode().name(),
            statContext.getMember().getDungeons().getSelectedClass().name(),
            now.getSeason().name(),
            now.getHour(),
            Concurrent.newUnmodifiableMap()
        );
    }

    /**
     * Resolves everything this compute can know, group counts included.
     *
     * <p>
     * The sheet must already be filled, because a group is counted off what the flat pass put in the
     * slots. A count taken before the slots are open is a count of nothing.
     *
     * @param statContext the member and the island to read from
     * @param sheet the filled sheet, read for what the member is wearing
     * @return the facts, read once
     */
    static @NotNull ComputeFacts of(@NotNull StatContext statContext, @NotNull StatSheet sheet) {
        SkyBlockDate now = new SkyBlockDate(System.currentTimeMillis());

        return new ComputeFacts(
            statContext.getIsland().getGameMode().name(),
            statContext.getMember().getDungeons().getSelectedClass().name(),
            now.getSeason().name(),
            now.getHour(),
            countGroups(sheet)
        );
    }

    /**
     * Counts how many members of each declared group the member is wearing.
     *
     * <p>
     * Every declared group is counted rather than only the ones some row goes on to ask about,
     * because a {@code COUNT} term names a group id and nothing says in advance which ids a condition
     * will reach. A group nobody wears counts zero and is present, which is what lets a gate read
     * "fewer than four" rather than reading nothing at all.
     *
     * @param sheet the filled sheet
     * @return the counts, keyed by group id
     */
    private static @NotNull ConcurrentMap<String, Integer> countGroups(@NotNull StatSheet sheet) {
        ConcurrentList<Buff> declaring = BuffEvaluator.selectAll(Buff.Subject.Kind.GROUP);

        if (declaring.isEmpty())
            return Concurrent.newUnmodifiableMap();

        ConcurrentList<String> worn = sheet.getSlots()
            .values()
            .stream()
            .map(slot -> slot.stack().getItem().getId())
            .collect(Concurrent.toUnmodifiableList());

        ConcurrentMap<String, Integer> counts = Concurrent.newMap();

        declaring.stream()
            .filter(row -> row.getGroupId() != null)
            .forEach(row -> counts.put(row.getGroupId(), (int) worn.stream().filter(row.getMemberIds()::contains).count()));

        return counts;
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

        for (Map.Entry<String, Integer> counted : this.groupCounts().entrySet())
            context.groupCount(counted.getKey(), counted.getValue());

        return context;
    }

}
