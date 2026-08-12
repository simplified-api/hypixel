package api.simplified.hypixel.response.skyblock;

import api.simplified.skyblock.common.Profile;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Every profile one player is a member of.
 * <p>
 * Each profile arrives whole, with its members and their state, so this is the response the rest of
 * the tree hangs off. What a member exposes is theirs to decide: a subtree hidden through their
 * in-game API settings simply does not arrive, and every collection below defaults non-null so that
 * absence never reaches a caller as null.
 */
@Getter
public class SkyBlockProfiles {

    /**
     * Whether the request was served.
     */
    private boolean success;

    /**
     * The player's profiles, empty rather than null for a player who has never played SkyBlock.
     */
    @SerializedName("profiles")
    private @NotNull ConcurrentList<SkyBlockIsland> islands = Concurrent.newList();

    /**
     * Finds the profile carrying a given fruit name.
     *
     * @param profile the fruit name to match
     * @return the profile of that name, empty when the player holds none
     */
    public @NotNull Optional<SkyBlockIsland> getIsland(@NotNull Profile profile) {
        return this.getIsland(profile.name());
    }

    /**
     * Finds the profile carrying a given fruit name, matched without regard to case.
     *
     * @param profileName the fruit name to match
     * @return the profile of that name, empty when the player holds none
     */
    public @NotNull Optional<SkyBlockIsland> getIsland(@NotNull String profileName) {
        return this.getIslands()
            .stream()
            .filter(skyBlockIsland -> skyBlockIsland.getProfile()
                .name()
                .equalsIgnoreCase(profileName)
            )
            .findFirst();
    }

    /**
     * The profile the player last played, falling back to the first the wire listed when none is
     * marked.
     */
    public @NotNull SkyBlockIsland getSelected() {
        return this.getIslands()
            .stream()
            .filter(SkyBlockIsland::isSelected)
            .findFirst()
            .orElse(this.getIslands().getFirst());
    }

}
