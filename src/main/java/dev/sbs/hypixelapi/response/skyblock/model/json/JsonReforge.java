package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.common.Rarity;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Reforge;
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
    name = "reforges"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonReforge implements Reforge, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Optional<String> stoneId = Optional.empty();
    private int requiredLevel = 0;
    private @NotNull ConcurrentList<String> categoryIds = Concurrent.newList();
    private @NotNull ConcurrentList<String> itemIds = Concurrent.newList();
    private @NotNull ConcurrentList<JsonSubstitute> stats = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonReforge that = (JsonReforge) o;

        return new EqualsBuilder()
            .append(this.getRequiredLevel(), that.getRequiredLevel())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getStoneId(), that.getStoneId())
            .append(this.getCategoryIds(), that.getCategoryIds())
            .append(this.getItemIds(), that.getItemIds())
            .append(this.getStats(), that.getStats())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getStoneId())
            .append(this.getRequiredLevel())
            .append(this.getCategoryIds())
            .append(this.getItemIds())
            .append(this.getStats())
            .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonSubstitute implements Substitute {

        private @NotNull String id = "";
        private @NotNull ConcurrentMap<Rarity, Double> values = Concurrent.newMap();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonSubstitute that = (JsonSubstitute) o;

            return new EqualsBuilder()
                .append(this.getId(), that.getId())
                .append(this.getValues(), that.getValues())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getValues())
                .build();
        }

    }

}
