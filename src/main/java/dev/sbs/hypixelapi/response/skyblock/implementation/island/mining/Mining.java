package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.mining;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.util.SkyBlockDate;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.stream.pair.Pair;
import dev.sbs.api.util.NumberUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

@Getter
public class Mining {

    private @NotNull ConcurrentMap<String, Object> nodes = Concurrent.newMap();
    @Accessors(fluent = true)
    @SerializedName("received_free_tier")
    private boolean hasReceivedFreeTier;
    private double experience;
    @SerializedName("last_reset")
    private @NotNull Optional<SkyBlockDate.RealTime> lastReset = Optional.empty();
    @SerializedName("greater_mines_last_access")
    private @NotNull Optional<SkyBlockDate.RealTime> lastAccessToGreaterMines = Optional.empty();
    @SerializedName("current_daily_effect")
    private Optional<String> currentSkymallEffect = Optional.empty();
    @SerializedName("current_daily_effect_last_changed")
    private int skymallEffectLastChanged;

    // Tokens
    @SerializedName("tokens")
    private int remainingTokens;
    @SerializedName("tokens_spent")
    private int spentTokens;
    @SerializedName("retroactive_tier2_token")
    private boolean retroactiveTier2Token;
    @SerializedName("selected_pickaxe_ability")
    private Optional<String> selectedPickaxeAbility = Optional.empty();
    private @NotNull ConcurrentMap<Crystal.Type, Crystal> crystals = Concurrent.newMap();

    // Powder
    @SerializedName("powder_mithril")
    private int mithrilPowder;
    @SerializedName("powder_mithril_total")
    private int totalMithrilPowder;
    @SerializedName("powder_spent_mithril")
    private int spentMithrilPowder;
    @SerializedName("powder_gemstone")
    private int gemstonePowder;
    @SerializedName("powder_gemstone_total")
    private int totalGemstonePowder;
    @SerializedName("powder_spent_gemstone")
    private int spentGemstonePowder;

    // Daily Ores
    @SerializedName("daily_ores_mined")
    private int dailyOresMined;
    @SerializedName("daily_ores_mined_day")
    private int dailyOresMinedDay;
    @SerializedName("daily_ores_mined_mithril_ore")
    private int dailyOresMinedMithrilOre;
    @SerializedName("daily_ores_mined_day_mithril_ore")
    private int dailyOresMinedDayMithrilOre;
    @SerializedName("daily_ores_mined_gemstone")
    private int dailyOresMinedGemstone;
    @SerializedName("daily_ores_mined_day_gemstone")
    private int dailyOresMinedDayGemstone;

    // Biomes
    @SerializedPath("biomes.dwarven")
    private @NotNull Biome.Dwarven dwarvenMinesBiome = new Biome.Dwarven();
    @SerializedPath("biomes.precursor")
    private @NotNull Biome.Precursor precursorCityBiome = new Biome.Precursor();
    @SerializedPath("biomes.goblin")
    private @NotNull Biome.Goblin goblinHideoutBiome = new Biome.Goblin();

    public @NotNull Crystal getCrystal(@NotNull Crystal.Type crystalType) {
        return this.crystals.get(crystalType);
    }

    public @NotNull ConcurrentMap<String, Double> getNodes() {
        return this.nodes.stream()
            .filter(entry -> !(entry.getValue() instanceof Boolean))
            .collect(Concurrent.toMap(Map.Entry::getKey, entry -> NumberUtil.createDouble(entry.getValue().toString())));
    }

    public @NotNull ConcurrentMap<String, Boolean> getToggles() {
        return this.nodes.stream()
            .filter(entry -> (entry.getValue() instanceof Boolean))
            .map(entry -> Pair.of(entry.getKey().replace("toggle_", ""), (boolean) entry.getValue()))
            .collect(Concurrent.toMap());
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Biome {

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Dwarven extends Biome {

            @SerializedName("statues_placed")
            private @NotNull ConcurrentList<Object> placedStatues = Concurrent.newList();

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Precursor extends Biome {

            @SerializedName("parts_delivered")
            private @NotNull ConcurrentList<Object> deliveredParts = Concurrent.newList();

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Goblin extends Biome {

            @SerializedName("king_quest_active")
            private boolean kingQuestActive;
            @SerializedName("king_quests_completed")
            private int completedKingQuests;

        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Crystal {

        private @NotNull State state = State.NOT_FOUND;
        @SerializedName("total_placed")
        private int totalPlaced;

        public enum State {

            FOUND,
            NOT_FOUND

        }

        public enum Type {

            @SerializedName("jade_crystal")
            JADE,
            @SerializedName("amber_crystal")
            AMBER,
            @SerializedName("topaz_crystal")
            TOPAZ,
            @SerializedName("sapphire_crystal")
            SAPHIRE,
            @SerializedName("amethyst_crystal")
            AMETHYST,
            @SerializedName("jasper_crystal")
            JASPER,
            @SerializedName("ruby_crystal")
            RUBY

        }

    }

}
