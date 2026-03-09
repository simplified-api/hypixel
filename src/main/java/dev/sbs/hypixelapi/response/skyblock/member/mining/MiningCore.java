package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class MiningCore implements PostInit {

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
    private transient @NotNull Powder powder = new Powder();
    @Getter(AccessLevel.NONE)
    private int powder_mithril;
    @Getter(AccessLevel.NONE)
    private int powder_mithril_total;
    @Getter(AccessLevel.NONE)
    private int powder_spent_mithril;
    @Getter(AccessLevel.NONE)
    private int powder_gemstone;
    @Getter(AccessLevel.NONE)
    private int powder_gemstone_total;
    @Getter(AccessLevel.NONE)
    private int powder_spent_gemstone;
    @Getter(AccessLevel.NONE)
    private int powder_glacite;
    @Getter(AccessLevel.NONE)
    private int powder_glacite_total;
    @Getter(AccessLevel.NONE)
    private int powder_spent_glacite;

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

    @Override
    public void postInit() {
        this.powder = new Powder(
            this.powder_mithril, this.powder_mithril_total, this.powder_spent_mithril,
            this.powder_gemstone, this.powder_gemstone_total, this.powder_spent_gemstone,
            this.powder_glacite, this.powder_glacite_total, this.powder_spent_glacite
        );
    }



}
