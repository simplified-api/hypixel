package dev.sbs.minecraftapi.client.hypixel.response.resource;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class ResourceSkillsResponse {

    private boolean success;
    private long lastUpdated;
    private String version;
    private @NotNull ConcurrentMap<String, Skill> collections = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Skill> skills = Concurrent.newMap();

    @Getter
    public static class Skill {

        private String name;
        private String description;
        private int maxLevel;
        private @NotNull ConcurrentList<SkillLevel> levels = Concurrent.newList();

    }

    @Getter
    public static class SkillLevel {

        private int level;
        private double totalExpRequired;
        private @NotNull ConcurrentList<String> unlocks = Concurrent.newList();

    }

}
