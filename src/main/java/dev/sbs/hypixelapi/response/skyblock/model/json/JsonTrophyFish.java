package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.TrophyFish;
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
    name = "trophy_fishes"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonTrophyFish implements TrophyFish, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull ChatFormat format = ChatFormat.WHITE;
    @SerializedName("zone")
    private @NotNull Optional<String> zoneId = Optional.empty();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonTrophyFish that = (JsonTrophyFish) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getFormat(), that.getFormat())
            .append(this.getZoneId(), that.getZoneId())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getFormat())
            .append(this.getZoneId())
            .build();
    }

}
