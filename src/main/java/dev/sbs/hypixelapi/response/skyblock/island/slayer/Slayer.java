package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.slayer;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

public class Slayer implements PostInit {

    @SerializedName("slayer_quest")
    @Getter private @NotNull Optional<Quest> activeQuest = Optional.empty();
    @SerializedName("slayer_bosses")
    private @NotNull ConcurrentMap<Type, SlayerBoss> bossMap = Concurrent.newMap();
    @Getter private @NotNull ConcurrentList<SlayerBoss> bosses = Concurrent.newList();

    public @NotNull SlayerBoss getBoss(@NotNull Type type) {
        return this.getBosses()
            .stream()
            .filter(boss -> boss.getType() == type)
            .findFirst()
            .orElseThrow();
    }

    @Override
    public void postInit() {
        this.bossMap.forEach((type, boss) -> boss.type = type);
        this.bosses = Concurrent.newUnmodifiableList(this.bossMap.values());
    }

    @Getter
    public static class Quest {

        private Type type;
        private int tier;
        @SerializedName("start_timestamp")
        private Instant start;
        @SerializedName("completion_state")
        private int completionState;
        @SerializedName("used_armor")
        private boolean usedArmor;
        private boolean solo;

    }

    public enum Type {

        UNKNOWN,
        @SerializedName("ZOMBIE")
        REVENANT_HORROR,
        @SerializedName("SPIDER")
        TARANTULA_BROODFATHER,
        @SerializedName("WOLF")
        SVEN_PACKMASTER,
        @SerializedName("ENDERMAN")
        VOIDGLOOM_SERAPH,
        @SerializedName("BLAZE")
        INFERNO_DEMONLORD,
        @SerializedName("VAMPIRE")
        RIFTSTALKER_BLOODFIEND;

        /*public @NotNull SlayerModel getModel() {
            if (this == UNKNOWN)
                throw new UnsupportedOperationException("Unknown does not exist in the database!");

            return SimplifiedApi.getRepositoryOf(SlayerModel.class).findFirstOrNull(SlayerModel::getKey, this.name());
        }*/

        public static @NotNull Type of(@NotNull String name) {
            return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(UNKNOWN);
        }

    }

}
