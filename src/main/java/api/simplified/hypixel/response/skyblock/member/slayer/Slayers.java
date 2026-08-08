package api.simplified.hypixel.response.skyblock.member.slayer;

import api.simplified.hypixel.common.WeightedGroup;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Collapse;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class Slayers implements WeightedGroup<SlayerBoss> {

    @SerializedName("slayer_quest")
    private final @NotNull Optional<SlayerQuest> activeQuest = Optional.empty();
    @Collapse
    @SerializedName("slayer_bosses")
    private @NotNull ConcurrentList<SlayerBoss> bosses = Concurrent.newList();

    /** {@inheritDoc} */
    @Override
    public @NotNull ConcurrentList<SlayerBoss> getWeighted() {
        return this.getBosses();
    }

}
