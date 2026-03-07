package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class Crystal {

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
