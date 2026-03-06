package dev.sbs.minecraftapi.client.hypixel.response.skyblock.garden;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ComposterData {

    @SerializedName("organic_matter")
    private double organicMatter;
    @SerializedName("fuel_units")
    private double fuelUnits;
    @SerializedName("compost_units")
    private int compostUnits;
    @SerializedName("compost_items")
    private int compostItems;
    @SerializedName("conversion_ticks")
    private int conversionTicks;
    @SerializedName("last_save")
    private SkyBlockDate.RealTime lastSave;
    private Upgrades upgrades = new Upgrades();

    @Getter
    @NoArgsConstructor
    public static class Upgrades {

        private int speed;
        @SerializedName("multi_drop")
        private int multiDrop;
        @SerializedName("fuel_cap")
        private int fuelCap;
        @SerializedName("organic_matter_cap")
        private int organicMatterCap;
        @SerializedName("cost_reduction")
        private int costReduction;

    }

}
