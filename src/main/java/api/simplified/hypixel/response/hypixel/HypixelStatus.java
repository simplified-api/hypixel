package api.simplified.hypixel.response.hypixel;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.Getter;

import java.util.Optional;
import java.util.UUID;

/**
 * A player's current presence on the network - whether they are online and, if so, what they are
 * playing.
 */
@Getter
public class HypixelStatus {

    /**
     * Whether the wire reported the request as successful.
     */
    private boolean success;

    /**
     * Minecraft unique id of the player the status belongs to.
     */
    @SerializedName("uuid")
    private UUID uniqueId;

    /**
     * The player's session, falling back to an offline session when the wire carries none.
     */
    private Session session = Session.UNKNOWN;

    /**
     * A player's current session on the network.
     */
    @Getter
    public static class Session {

        /**
         * The session stood in when the wire carries none, reporting the player offline and in no
         * game.
         */
        private static Session UNKNOWN = new Session();

        /**
         * Whether the player is currently online.
         */
        private boolean online;

        /**
         * Type name of the game the player is in, absent when they are in none.
         */
        private Optional<String> gameType = Optional.empty();

        /**
         * Mode within the game the player is in, absent when they are in none.
         */
        private Optional<String> mode = Optional.empty();

    }

}
