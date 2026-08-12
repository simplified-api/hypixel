package api.simplified.hypixel.response.skyblock.member.foraging;

import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * A member's record on the harp in front of Melody, on Melody's Plateau.
 *
 * <p>
 * Each song played through grants intelligence and SkyBlock experience once, and finishing the
 * hardest song awards the Melody's Hair accessory. Bound from {@code foraging.songs.harp}: the
 * selected song, when it was selected, the accessory flag and a play record for each song.
 *
 * <p>
 * The Prodigy songs of the Personal Harp on a member's private island land in this same node, so the
 * song map is wider than the list of songs Melody herself offers.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Harp">Harp</a>
 */
@Getter
public class MelodyHarp {

    /**
     * Whether the Melody's Hair accessory has been taken.
     *
     * <p>
     * The name follows the wire key {@code claimed_talisman}; the reward is an accessory.
     */
    @SerializedName("claimed_talisman")
    private boolean talismanClaimed;

    /**
     * The song currently loaded on the harp.
     *
     * <p>
     * This key sits inside the captured object without being captured: the song filter is anchored
     * on {@code song_} and {@code selected_song} does not begin with it.
     */
    @SerializedName("selected_song")
    private @NotNull Optional<String> selectedSong = Optional.empty();

    /**
     * When the selected song was chosen.
     *
     * <p>
     * Defaults to the epoch rather than to absent, so a member who has never touched the harp reads
     * as zero rather than as having no value at all.
     */
    @SerializedName("selected_song_epoch")
    private @NotNull SkyBlockDate.RealTime selectedSongTimestamp = new SkyBlockDate.RealTime(0);

    /**
     * A play record per song, keyed by the song's wire stem.
     *
     * <p>
     * Every key on the node beginning {@code song_} is captured here with that prefix stripped, and
     * what remains is split on the suffixes {@link Song} declares. The stem left over is what keys
     * the map.
     */
    @Capture(filter = "^song_")
    private @NotNull ConcurrentMap<String, Song> songs = Concurrent.newMap();

    /**
     * One song's play record.
     *
     * <p>
     * All three fields are <b>affix selectors inside the capturing map that holds them, not literal
     * wire keys</b>. The capture strips the leading {@code song_} and splits the remainder on the
     * suffixes declared here, so Hymn to the Joy arrives as the three keys
     * {@code song_hymn_joy_best_completion}, {@code song_hymn_joy_completions} and
     * {@code song_hymn_joy_perfect_completions}.
     */
    @Getter
    @NoArgsConstructor
    public static class Song {

        /**
         * The best result the member has posted on this song, as the wire scores it.
         */
        @SerializedName("best_completion")
        private double bestCompletion;

        /**
         * Times the song has been played through.
         */
        private int completions;

        /**
         * Times the song has been played through perfectly.
         */
        @SerializedName("perfect_completions")
        private int perfectCompletions;

    }

}
