package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Slayer;
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
    name = "slayers"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonSlayer implements Slayer, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    private int maxLevel = 9;
    private int maxTier = 5;
    @SerializedName("mobType")
    private @NotNull String mobTypeId = "";
    private double weightModifier;
    private int weightDivider;
    private @NotNull ConcurrentList<JsonSlayerLevel> levels = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonSlayer that = (JsonSlayer) o;

        return new EqualsBuilder()
            .append(this.getMaxLevel(), that.getMaxLevel())
            .append(this.getMaxTier(), that.getMaxTier())
            .append(this.getWeightModifier(), that.getWeightModifier())
            .append(this.getWeightDivider(), that.getWeightDivider())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getDescription(), that.getDescription())
            .append(this.getMobTypeId(), that.getMobTypeId())
            .append(this.getLevels(), that.getLevels())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(this.getMaxLevel())
            .append(this.getMaxTier())
            .append(this.getMobTypeId())
            .append(this.getWeightModifier())
            .append(this.getWeightDivider())
            .append(this.getLevels())
            .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonSlayerLevel implements Level {

        private int level;
        private double totalRequiredXP;
        private @NotNull String title = "";
        private @NotNull ConcurrentList<String> unlocks = Concurrent.newList();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonSlayerLevel that = (JsonSlayerLevel) o;

            return new EqualsBuilder()
                .append(this.getLevel(), that.getLevel())
                .append(this.getTotalRequiredXP(), that.getTotalRequiredXP())
                .append(this.getTitle(), that.getTitle())
                .append(this.getUnlocks(), that.getUnlocks())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getLevel())
                .append(this.getTotalRequiredXP())
                .append(this.getTitle())
                .append(this.getUnlocks())
                .build();
        }

    }

}
