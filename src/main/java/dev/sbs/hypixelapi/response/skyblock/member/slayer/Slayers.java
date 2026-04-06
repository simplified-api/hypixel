package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.slayer;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.common.Weight;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.collection.tuple.pair.Pair;
import dev.simplified.gson.Collapse;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class Slayers {

    @SerializedName("slayer_quest")
    private final @NotNull Optional<SlayerQuest> activeQuest = Optional.empty();
    @Collapse
    @SerializedName("slayer_bosses")
    private @NotNull ConcurrentList<SlayerBoss> bosses = Concurrent.newList();

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
