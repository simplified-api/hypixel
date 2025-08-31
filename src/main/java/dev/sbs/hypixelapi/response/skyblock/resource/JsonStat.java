package dev.sbs.minecraftapi.client.hypixel.response.skyblock.resource;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.data.json.JsonModel;
import dev.sbs.minecraftapi.skyblock.resource.Stat;
import dev.sbs.minecraftapi.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonStat implements Stat, JsonModel {

    private @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String symbol = "";
    private @NotNull ChatFormat format = ChatFormat.WHITE;
    private @NotNull String category = "";
    private double base;
    private double cap;
    private double tuningMultiplier;
    private boolean visible;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonStat jsonStat = (JsonStat) o;

        return new EqualsBuilder()
            .append(this.getBase(), jsonStat.getBase())
            .append(this.getCap(), jsonStat.getCap())
            .append(this.getTuningMultiplier(), jsonStat.getTuningMultiplier())
            .append(this.isVisible(), jsonStat.isVisible())
            .append(this.getId(), jsonStat.getId())
            .append(this.getName(), jsonStat.getName())
            .append(this.getSymbol(), jsonStat.getSymbol())
            .append(this.getFormat(), jsonStat.getFormat())
            .append(this.getCategory(), jsonStat.getCategory())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getSymbol())
            .append(this.getFormat())
            .append(this.getCategory())
            .append(this.getBase())
            .append(this.getCap())
            .append(this.getTuningMultiplier())
            .append(this.isVisible())
            .build();
    }

}
