package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.Capture;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class HeartOfTheMountain {

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
    @Capture(filter = "^powder_")
    private @NotNull ConcurrentMap<Powder.Type, Powder> powder = Concurrent.newMap();

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

}
