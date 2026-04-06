package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.common.Profile;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * All profiles a specific player belongs to.
 */
@Getter
public class SkyBlockProfiles {

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
                .name()
                .equalsIgnoreCase(profileName)
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
