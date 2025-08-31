package dev.sbs.minecraftapi.client.hypixel.response.skyblock.resource;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.data.json.JsonModel;
import dev.sbs.minecraftapi.skyblock.resource.Collection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonCollection implements Collection, JsonModel {

    protected @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull ConcurrentMap<String, JsonCollectionItem> items = Concurrent.newMap();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonCollection that = (JsonCollection) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getItems(), that.getItems())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getItems())
            .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonCollectionItem implements Collection.Item {

        private @NotNull String name = "";
        private int maxTiers;
        private @NotNull ConcurrentList<JsonCollectionTier> tiers = Concurrent.newList();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonCollectionItem that = (JsonCollectionItem) o;

            return new EqualsBuilder()
                .append(this.getMaxTiers(), that.getMaxTiers())
                .append(this.getName(), that.getName())
                .append(this.getTiers(), that.getTiers())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getName())
                .append(this.getMaxTiers())
                .append(this.getTiers())
                .build();
        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonCollectionTier implements Collection.Tier {

        private int tier;
        private int amountRequired;
        private @NotNull ConcurrentList<String> unlocks = Concurrent.newList();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonCollectionTier that = (JsonCollectionTier) o;

            return new EqualsBuilder()
                .append(this.getTier(), that.getTier())
                .append(this.getAmountRequired(), that.getAmountRequired())
                .append(this.getUnlocks(), that.getUnlocks())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getTier())
                .append(this.getAmountRequired())
                .append(this.getUnlocks())
                .build();
        }

    }

}
