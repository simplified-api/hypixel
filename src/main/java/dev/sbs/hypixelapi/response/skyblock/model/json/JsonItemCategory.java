package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Item;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.ItemCategory;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "item_categories"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonItemCategory implements ItemCategory, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Item.Type type = Item.Type.OTHER;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonItemCategory that = (JsonItemCategory) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getType(), that.getType())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getType())
            .build();
    }

}
