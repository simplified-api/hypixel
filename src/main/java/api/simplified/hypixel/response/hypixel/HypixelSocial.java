package api.simplified.hypixel.response.hypixel;

import dev.simplified.annotations.Getter;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import org.jetbrains.annotations.NotNull;

/**
 * The social media accounts a player has linked to their Hypixel account.
 */
public class HypixelSocial {

    /**
     * Whether the player has been shown the prompt to link a social media account.
     */
    private boolean prompt;

    /**
     * The linked handle or address for each service, keyed by the service it belongs to.
     */
    @Getter private @NotNull ConcurrentMap<Type, String> links = Concurrent.newMap();

    /**
     * The services a player can link an account for.
     */
    public enum Type {

        /**
         * A linked Twitter account.
         */
        TWITTER,

        /**
         * A linked YouTube channel.
         */
        YOUTUBE,

        /**
         * A linked Instagram account.
         */
        INSTAGRAM,

        /**
         * A linked Twitch channel.
         */
        TWITCH,

        /**
         * A linked Discord account.
         */
        DISCORD,

        /**
         * A linked Hypixel Forums account.
         */
        HYPIXEL

    }

}
