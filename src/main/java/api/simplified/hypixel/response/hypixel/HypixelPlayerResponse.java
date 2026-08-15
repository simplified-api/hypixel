package api.simplified.hypixel.response.hypixel;

import dev.simplified.annotations.Getter;

import java.util.Optional;

/**
 * Response envelope wrapping a player lookup.
 */
@Getter
public class HypixelPlayerResponse {

    /**
     * Whether the wire reported the request as successful.
     */
    private boolean success;

    /**
     * The player that was matched, absent when the account has never joined the network.
     */
    private Optional<HypixelPlayer> player = Optional.empty();

}
