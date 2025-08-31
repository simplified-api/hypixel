package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.skyblock.island.Profile;
import dev.sbs.minecraftapi.skyblock.island.SkyBlockIsland;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class SkyBlockProfilesResponse {

    private boolean success;
    @SerializedName("profiles")
    private @NotNull ConcurrentList<SkyBlockIsland> islands = Concurrent.newList();

    public @NotNull Optional<SkyBlockIsland> getIsland(@NotNull Profile profile) {
        return this.getIsland(profile.name());
    }

    public @NotNull Optional<SkyBlockIsland> getIsland(@NotNull String profileName) {
        return this.getIslands()
            .stream()
            .filter(skyBlockIsland -> skyBlockIsland.getProfile()
                .map(islandProfile -> islandProfile.name().equalsIgnoreCase(profileName))
                .orElse(false)
            )
            .findFirst();
    }

    public @NotNull SkyBlockIsland getSelected() {
        return this.getIslands()
            .stream()
            .filter(SkyBlockIsland::isSelected)
            .findFirst()
            .orElse(this.getIslands().get(0));
    }

}
