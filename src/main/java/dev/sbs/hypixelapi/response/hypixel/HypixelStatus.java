package dev.sbs.minecraftapi.client.hypixel.response.hypixel;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.Optional;
import java.util.UUID;

/**
 * Current online status of a player.
 */
@Getter
public class HypixelStatus {

    private boolean success;
    @SerializedName("uuid")
    private UUID uniqueId;
    private Session session = Session.UNKNOWN;

    @Getter
    public static class Session {

        private static Session UNKNOWN = new Session();
        private boolean online;
        private Optional<String> gameType = Optional.empty();
        private Optional<String> mode = Optional.empty();

    }

}
