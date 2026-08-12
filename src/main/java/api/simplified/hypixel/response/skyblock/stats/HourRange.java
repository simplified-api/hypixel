package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.skyblock.date.SkyBlockDate;
import dev.simplified.util.NumberUtil;
import org.jetbrains.annotations.NotNull;

/**
 * One inclusive span of SkyBlock hours a buff holds within.
 *
 * @param start the first hour the buff holds
 * @param end the last hour the buff holds
 */
public record HourRange(int start, int end) {

    /**
     * Reads one {@code start-end} pair.
     *
     * @param range the pair as the reference data spells it
     * @return the parsed range
     */
    public static @NotNull HourRange parse(@NotNull String range) {
        String[] parts = range.split("-");
        return new HourRange(NumberUtil.toInt(parts[0]), NumberUtil.toInt(parts[1]));
    }

    /**
     * Whether an hour falls inside this range.
     *
     * @param hour the hour to test
     * @return whether the buff holds at that hour
     */
    public boolean contains(int hour) {
        return hour >= this.start() && hour <= this.end();
    }

    /**
     * Whether the range covers the hour it is currently.
     *
     * @return whether the buff holds now
     */
    public boolean containsNow() {
        return this.contains(new SkyBlockDate(System.currentTimeMillis()).getHour());
    }

}
