package dev.sbs.minecraftapi.client.hypixel.response.skyblock.resource;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.data.json.JsonModel;
import dev.sbs.minecraftapi.skyblock.resource.SlayerExtra;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonSlayerExtra implements SlayerExtra, JsonModel {

    protected @NotNull String id = "";
    private double weightModifier;
    private int weightDivider;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonSlayerExtra that = (JsonSlayerExtra) o;

        return new EqualsBuilder()
            .append(this.getWeightModifier(), that.getWeightModifier())
            .append(this.getWeightDivider(), that.getWeightDivider())
            .append(this.getId(), that.getId())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getWeightModifier())
            .append(this.getWeightDivider())
            .build();
    }

}
