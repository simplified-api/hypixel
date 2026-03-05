package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

@Getter
public class Mining implements PostInit {

    private @NotNull ConcurrentMap<String, Object> nodes = Concurrent.newMap();
    @Accessors(fluent = true)
    @SerializedName("received_free_tier")
    private boolean hasReceivedFreeTier;
    private double experience;
    @SerializedName("current_daily_effect")
    private Optional<String> currentSkymallEffect = Optional.empty();
    @SerializedName("current_daily_effect_last_changed")
    private int skymallEffectLastChanged;
    @SerializedName("biomes")
    private @NotNull CrystalHollows crystalHollows = new CrystalHollows();

    // Time
    @SerializedName("last_reset")
    private @NotNull Optional<SkyBlockDate.RealTime> lastReset = Optional.empty();
    @SerializedName("greater_mines_last_access")
    private @NotNull Optional<SkyBlockDate.RealTime> lastAccessToGreaterMines = Optional.empty();

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
    private @NotNull Powder powder = new Powder(
        0, 0, 0,
        0, 0, 0,
        0, 0, 0
    );
    @Getter(AccessLevel.NONE)
    @SerializedName("powder_mithril")
    private int mithrilPowder;
    @Getter(AccessLevel.NONE)
    @SerializedName("powder_mithril_total")
    private int totalMithrilPowder;
    @Getter(AccessLevel.NONE)
    @SerializedName("powder_spent_mithril")
    private int spentMithrilPowder;
    @Getter(AccessLevel.NONE)
    @SerializedName("powder_gemstone")
    private int gemstonePowder;
    @Getter(AccessLevel.NONE)
    @SerializedName("powder_gemstone_total")
    private int totalGemstonePowder;
    @Getter(AccessLevel.NONE)
    @SerializedName("powder_spent_gemstone")
    private int spentGemstonePowder;
    @Getter(AccessLevel.NONE)
    @SerializedName("powder_glacite")
    private int glacitePowder;
    @Getter(AccessLevel.NONE)
    @SerializedName("powder_glacite_total")
    private int totalGlacitePowder;
    @Getter(AccessLevel.NONE)
    @SerializedName("powder_spent_glacite")
    private int spentGlacitePowder;

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
    @SerializedName("daily_ores_mined_glacite")
    private int dailyOresMinedGlacite;
    @SerializedName("daily_ores_mined_day_glacite")
    private int dailyOresMinedDayGlacite;

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

    @Override
    public void postInit() {
        this.powder = new Powder(
            this.mithrilPowder, this.totalMithrilPowder, this.spentMithrilPowder,
            this.gemstonePowder, this.totalGemstonePowder, this.spentGemstonePowder,
            this.glacitePowder, this.totalGlacitePowder, this.spentGlacitePowder
        );
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CrystalHollows {

        @SerializedName("dwarven")
        private @NotNull MinesOfDivan minesOfDivan = new MinesOfDivan();
        @SerializedName("precursor")
        private @NotNull LostPrecursorCity lostPrecursorCity = new LostPrecursorCity();
        @SerializedPath("goblin")
        private @NotNull GoblinHoldout goblinHoldout = new GoblinHoldout();
        @SerializedPath("jungle")
        private @NotNull JungleTemple jungleTemple = new JungleTemple();

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class MinesOfDivan {

            @SerializedName("statues_placed")
            private @NotNull ConcurrentList<Object> placedStatues = Concurrent.newList();

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class LostPrecursorCity {

            @SerializedName("parts_delivered")
            private @NotNull ConcurrentList<Object> deliveredParts = Concurrent.newList();

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class GoblinHoldout {

            @SerializedName("king_quest_active")
            private boolean kingQuestActive;
            @SerializedName("king_quests_completed")
            private int completedKingQuests;

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class JungleTemple {

            @SerializedName("jungle_temple_open")
            private boolean open;
            @SerializedName("jungle_temple_chest_uses")
            private int chestUses;

        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Crystal {

        private @NotNull State state = State.NOT_FOUND;
        @SerializedName("total_placed")
        private int totalPlaced;
        @SerializedName("total_found")
        private int totalFound;

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
            RUBY,

            @SerializedName("onyx_crystal")
            ONYX,
            @SerializedName("aquamarine_crystal")
            AQUAMARINE,
            @SerializedName("opal_crystal")
            OPAL,
            @SerializedName("citrine_crystal")
            CITRINE,
            @SerializedName("peridot_crystal")
            PERIDOT

        }

    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Powder {

        private final @NotNull ConcurrentMap<OreType, Data> oreTypes;

        private Powder(int mithrilAmount, int mithrilTotal, int mithrilSpent, int gemstoneAmount, int gemstoneTotal, int gemstoneSpent, int glaciteAmount, int glaciteTotal, int glaciteSpent) {
            ConcurrentMap<OreType, Data> oreTypes = Concurrent.newMap();
            oreTypes.put(OreType.MITHRIL, new Data(mithrilAmount, mithrilTotal, mithrilSpent));
            oreTypes.put(OreType.GEMSTONE, new Data(gemstoneAmount, gemstoneTotal, gemstoneSpent));
            oreTypes.put(OreType.GLACITE, new Data(glaciteAmount, glaciteTotal, glaciteSpent));
            this.oreTypes = oreTypes.toUnmodifiableMap();
        }

        @Getter
        @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Data {

            private final int amount;
            private final int total;
            private final int spent;

        }

    }

    public enum OreType {

        MITHRIL,
        GEMSTONE,
        GLACITE

    }

}
