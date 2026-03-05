package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.BitsItem;
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
    name = "bits_items"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonBitsItem implements BitsItem, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull Type type = Type.ITEM;
    private int cost;
    private @NotNull ConcurrentList<Variant> variants = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonBitsItem that = (JsonBitsItem) o;

        return new EqualsBuilder()
            .append(this.getCost(), that.getCost())
            .append(this.getId(), that.getId())
            .append(this.getType(), that.getType())
            .append(this.getVariants(), that.getVariants())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getType())
            .append(this.getCost())
            .append(this.getVariants())
            .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonVariant implements Variant {

        private @NotNull String id = "";
        private int cost;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonVariant that = (JsonVariant) o;

            return new EqualsBuilder()
                .append(this.getCost(), that.getCost())
                .append(this.getId(), that.getId())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getCost())
                .build();
        }

    }

}
