package dev.sbs.minecraftapi.client.hypixel.response.skyblock.resource;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.reflection.Reflection;
import dev.sbs.minecraftapi.skyblock.data.json.JsonSkill;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class ResourceSkillsResponse implements PostInit {

    private static final @NotNull Reflection<JsonSkill> REFLECTION = Reflection.of(JsonSkill.class);
    private boolean success;
    private long lastUpdated;
    private String version;
    @SerializedName("skills")
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, JsonSkill> skillsMap = Concurrent.newMap();
    private transient @NotNull ConcurrentList<JsonSkill> skills = Concurrent.newList();

    @Override
    public void postInit() {
        this.skills = this.skillsMap.stream()
            .map((id, skill) -> {
                REFLECTION.setValue("id", skill, id.toUpperCase());
                return skill;
            })
            .collect(Concurrent.toUnmodifiableList());
    }

}
