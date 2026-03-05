package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.common.Rarity;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Pet;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Stat;
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
    name = "pets"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonPet implements Pet, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Rarity lowestRarity = Rarity.COMMON;
    private @NotNull String skillId = "";
    private @NotNull Type type = Type.PET;
    private int maxLevel = 100;
    private boolean passive = false;
    private @NotNull ConcurrentList<JsonSubstitute> stats = Concurrent.newList();
    private @NotNull ConcurrentList<JsonAbility> abilities = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonPet jsonPet = (JsonPet) o;

        return new EqualsBuilder()
            .append(this.getMaxLevel(), jsonPet.getMaxLevel())
            .append(this.isPassive(), jsonPet.isPassive())
            .append(this.getId(), jsonPet.getId())
            .append(this.getName(), jsonPet.getName())
            .append(this.getLowestRarity(), jsonPet.getLowestRarity())
            .append(this.getSkillId(), jsonPet.getSkillId())
            .append(this.getType(), jsonPet.getType())
            .append(this.getStats(), jsonPet.getStats())
            .append(this.getAbilities(), jsonPet.getAbilities())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getLowestRarity())
            .append(this.getSkillId())
            .append(this.getType())
            .append(this.getMaxLevel())
            .append(this.isPassive())
            .append(this.getStats())
            .append(this.getAbilities())
            .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonAbility implements Ability {

        private @NotNull String name = "";
        private @NotNull String description = "";
        private boolean flatStat = false;
        private @NotNull ConcurrentList<JsonSubstitute> stats = Concurrent.newList();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonAbility that = (JsonAbility) o;

            return new EqualsBuilder()
                .append(this.isFlatStat(), that.isFlatStat())
                .append(this.getName(), that.getName())
                .append(this.getDescription(), that.getDescription())
                .append(this.getStats(), that.getStats())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getName())
                .append(this.getDescription())
                .append(this.isFlatStat())
                .append(this.getStats())
                .build();
        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonSubstitute implements Substitute {

        private @NotNull String id = "";
        private int precision = 0;
        private @NotNull Stat.Type type = Stat.Type.NONE;
        private @NotNull ChatFormat format = ChatFormat.GREEN;
        private @NotNull ConcurrentMap<Rarity, JsonValue> values = Concurrent.newMap();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonSubstitute that = (JsonSubstitute) o;

            return new EqualsBuilder()
                .append(this.getPrecision(), that.getPrecision())
                .append(this.getId(), that.getId())
                .append(this.getType(), that.getType())
                .append(this.getFormat(), that.getFormat())
                .append(this.getValues(), that.getValues())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getPrecision())
                .append(this.getType())
                .append(this.getFormat())
                .append(this.getValues())
                .build();
        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.NONE)
        public static class JsonValue implements Value {

            private double base = 0.0;
            private double scalar = 0.0;

        }

    }

}
