package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.slayer;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.minecraftapi.skyblock.common.Weight;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class SlayerProgress implements PostInit {

    @SerializedName("slayer_quest")
    private final @NotNull Optional<SlayerQuest> activeQuest = Optional.empty();
    @Getter(AccessLevel.NONE)
    @SerializedName("slayer_bosses")
    private @NotNull ConcurrentMap<String, SlayerBoss> slayerBosses = Concurrent.newMap();
    private transient ConcurrentList<SlayerBoss> bosses = Concurrent.newList();

    @Override
    public void postInit() {
        this.bosses = this.slayerBosses.stream()
            .peek((id, slayerBoss) -> slayerBoss.setId(id))
            .mapToValue()
            .collect(Concurrent.toUnmodifiableList());
    }

    public @NotNull SlayerBoss getSlayer(@NotNull String id) {
        return this.getBosses()
            .matchFirst(skill -> skill.getId().equalsIgnoreCase(id))
            .orElseThrow(() -> new IllegalArgumentException("No boss with id " + id));
    }

    public double getAverage() {
        return this.getBosses()
            .stream()
            .mapToDouble(SlayerBoss::getLevel)
            .average()
            .orElse(0.0);
    }

    public double getExperience() {
        return this.getBosses()
            .stream()
            .mapToDouble(SlayerBoss::getExperience)
            .sum();
    }

    public double getProgressPercentage() {
        return this.getBosses()
            .stream()
            .mapToDouble(SlayerBoss::getTotalProgressPercentage)
            .average()
            .orElse(0.0);
    }

    public @NotNull ConcurrentMap<SlayerBoss, Weight> getWeight() {
        return this.getBosses()
            .stream()
            .map(slayer -> Pair.of(slayer, slayer.getWeight()))
            .collect(Concurrent.toMap());
    }

}
