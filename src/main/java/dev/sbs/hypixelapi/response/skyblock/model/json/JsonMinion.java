package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Minion;
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
    name = "minions"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonMinion implements Minion, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    @SerializedName("collection")
    private @NotNull String collectionId = "";
    private @NotNull ConcurrentList<JsonMinionTier> tiers = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonMinion that = (JsonMinion) o;

        return new EqualsBuilder()
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getCollectionId(), that.getCollectionId())
            .append(this.getTiers(), that.getTiers())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getCollectionId())
            .append(this.getTiers())
            .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonMinionTier implements Tier {

        private int tier;
        private double speed;
        @SerializedName("item")
        private @NotNull String itemId = "";
        private @NotNull JsonItem.JsonUpgradeCost upgradeCost = new JsonItem.JsonUpgradeCost();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonMinionTier that = (JsonMinionTier) o;

            return new EqualsBuilder()
                .append(this.getTier(), that.getTier())
                .append(this.getSpeed(), that.getSpeed())
                .append(this.getItemId(), that.getItemId())
                .append(this.getUpgradeCost(), that.getUpgradeCost())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getTier())
                .append(this.getSpeed())
                .append(this.getItemId())
                .append(this.getUpgradeCost())
                .build();
        }

    }

}
