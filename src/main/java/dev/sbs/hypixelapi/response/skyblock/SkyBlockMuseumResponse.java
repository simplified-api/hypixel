package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Museum data for an entire island.
 */
@Getter
public class SkyBlockMuseumResponse {

    private boolean success;
    @SerializedName("members")
    private @NotNull ConcurrentMap<UUID, SkyBlockMuseum> members = Concurrent.newMap();

}
