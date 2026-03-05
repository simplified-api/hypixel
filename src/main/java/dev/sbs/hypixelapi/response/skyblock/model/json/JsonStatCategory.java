package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.StatCategory;
import dev.sbs.minecraftapi.render.text.ChatFormat;
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
    name = "stat_categories"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonStatCategory implements StatCategory, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull ChatFormat format = ChatFormat.WHITE;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonStatCategory that = (JsonStatCategory) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getFormat(), that.getFormat())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getFormat())
            .build();
    }

}
