package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.skyblock.Museum;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public class SkyBlockMuseumResponse {

    private boolean success;
    @SerializedName("members")
    private @NotNull ConcurrentMap<UUID, Museum> members = Concurrent.newMap();

}
