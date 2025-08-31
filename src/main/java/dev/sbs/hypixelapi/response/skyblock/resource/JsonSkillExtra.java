package dev.sbs.minecraftapi.client.hypixel.response.skyblock.resource;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.data.json.JsonModel;
import dev.sbs.minecraftapi.skyblock.resource.SkillExtra;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonSkillExtra implements SkillExtra, JsonModel {

    protected @NotNull String id = "";
    private boolean cosmetic;
    private double weightExponent;
    private int weightDivider;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonSkillExtra that = (JsonSkillExtra) o;

        return new EqualsBuilder()
            .append(this.isCosmetic(), that.isCosmetic())
            .append(this.getWeightExponent(), that.getWeightExponent())
            .append(this.getWeightDivider(), that.getWeightDivider())
            .append(this.getId(), that.getId())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.isCosmetic())
            .append(this.getWeightExponent())
            .append(this.getWeightDivider())
            .build();
    }

}
