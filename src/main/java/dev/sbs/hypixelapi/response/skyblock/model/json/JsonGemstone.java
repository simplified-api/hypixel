package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Gemstone;
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
    name = "gemstones"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonGemstone implements Gemstone, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String symbol = "";
    private @NotNull ChatFormat format = ChatFormat.WHITE;
    @SerializedName("stat")
    private @NotNull String statId = "";
    private @NotNull ConcurrentMap<Type, ConcurrentMap<ChatFormat, Double>> values = Concurrent.newMap();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonGemstone that = (JsonGemstone) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getSymbol(), that.getSymbol())
            .append(this.getFormat(), that.getFormat())
            .append(this.getStatId(), that.getStatId())
            .append(this.getValues(), that.getValues())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getSymbol())
            .append(this.getFormat())
            .append(this.getStatId())
            .append(this.getValues())
            .build();
    }

}
