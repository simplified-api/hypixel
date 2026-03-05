package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.common.Rarity;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Accessory;
import dev.sbs.minecraftapi.client.mojang.profile.MojangProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
    name = "accessories",
    required = {
        JsonItem.class
    }
)
@NoArgsConstructor(access = AccessLevel.NONE, force = true)
public class JsonAccessory implements Accessory, JsonModel, PostInit {

    private @Id @NotNull String id;
    private @NotNull String name;
    private @NotNull Optional<String> description = Optional.empty();
    private @NotNull Rarity rarity = Rarity.COMMON;
    private @NotNull Source source = Source.MISCELLANEOUS;
    private @NotNull Limit limit = Limit.NONE;
    private @NotNull Optional<MojangProperty> skin = Optional.empty();
    private @NotNull JsonItem.JsonAttributes attributes = new JsonItem.JsonAttributes();
    private @NotNull Optional<Family> family = Optional.empty();
    private @NotNull ConcurrentList<Substitute> stats = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonAccessory that = (JsonAccessory) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getDescription(), that.getDescription())
            .append(this.getRarity(), that.getRarity())
            .append(this.getSource(), that.getSource())
            .append(this.getLimit(), that.getLimit())
            .append(this.getSkin(), that.getSkin())
            .append(this.getAttributes(), that.getAttributes())
            .append(this.getFamily(), that.getFamily())
            .append(this.getStats(), that.getStats())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(this.getRarity())
            .append(this.getSource())
            .append(this.getLimit())
            .append(this.getSkin())
            .append(this.getAttributes())
            .append(this.getFamily())
            .append(this.getStats())
            .build();
    }

    @Override
    public void postInit() {
        this.name = this.getItem().getDisplayName();
        this.rarity = this.getItem().getRarity();
        this.skin = this.getItem().getSkin();
        this.stats = this.getItem()
            .getStats()
            .stream()
            .mapKey(String::toUpperCase)
            .mapToObj(JsonSubstitute::new)
            .collect(Concurrent.toUnmodifiableList());
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE, force = true)
    public static class JsonFamily implements Accessory.Family {

        private @NotNull String id;
        private int rank;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonFamily that = (JsonFamily) o;

            return new EqualsBuilder()
                .append(this.getRank(), that.getRank())
                .append(this.getId(), that.getId())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getRank())
                .build();
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JsonSubstitute implements Accessory.Substitute {

        private @NotNull String id;
        private double value;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonSubstitute that = (JsonSubstitute) o;

            return new EqualsBuilder()
                .append(this.getValue(), that.getValue())
                .append(this.getId(), that.getId())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getValue())
                .build();
        }

    }

}
