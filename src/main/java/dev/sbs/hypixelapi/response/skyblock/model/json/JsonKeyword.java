package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Keyword;
import dev.sbs.minecraftapi.render.text.ChatFormat;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Optional;

@Getter
@Entity
@JsonResource(
    path = "skyblock",
    name = "keywords"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonKeyword implements Keyword, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Optional<String> plural = Optional.empty();
    private @NotNull Optional<String> symbol = Optional.empty();
    private @NotNull ChatFormat format = ChatFormat.GREEN;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonKeyword that = (JsonKeyword) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getPlural(), that.getPlural())
            .append(this.getSymbol(), that.getSymbol())
            .append(this.getFormat(), that.getFormat())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getPlural())
            .append(this.getSymbol())
            .append(this.getFormat())
            .build();
    }

}
