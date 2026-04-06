package dev.sbs.minecraftapi.client.hypixel.response.resource;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.persistence.model.Skill;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Information regarding Skills.
 */
@Getter
public class ResourceSkills {

    private boolean success;
    @SerializedName("lastUpdated")
    private @NotNull SkyBlockDate.RealTime lastUpdated;
    @SerializedName("version")
    private @NotNull String version;
    private @NotNull ConcurrentMap<String, Skill> skills = Concurrent.newMap();

    // TODO: Migrate away from JpaModel

}
