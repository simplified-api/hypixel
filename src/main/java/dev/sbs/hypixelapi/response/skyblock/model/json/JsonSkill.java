package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Skill;
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
    name = "skills"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonSkill implements Skill, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull String description = "";
    private int maxLevel = 50;
    private boolean cosmetic;
    private double weightExponent;
    private int weightDivider;
    private @NotNull ConcurrentList<JsonSkillLevel> levels = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonSkill jsonSkill = (JsonSkill) o;

        return new EqualsBuilder()
            .append(this.getMaxLevel(), jsonSkill.getMaxLevel())
            .append(this.isCosmetic(), jsonSkill.isCosmetic())
            .append(this.getWeightExponent(), jsonSkill.getWeightExponent())
            .append(this.getWeightDivider(), jsonSkill.getWeightDivider())
            .append(this.getId(), jsonSkill.getId())
            .append(this.getName(), jsonSkill.getName())
            .append(this.getDescription(), jsonSkill.getDescription())
            .append(this.getLevels(), jsonSkill.getLevels())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(this.getMaxLevel())
            .append(this.isCosmetic())
            .append(this.getWeightExponent())
            .append(this.getWeightDivider())
            .append(this.getLevels())
            .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonSkillLevel implements Skill.Level {

        private int level;
        private double totalRequiredXP;
        private @NotNull String title = "";
        private @NotNull ConcurrentList<String> unlocks = Concurrent.newList();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonSkillLevel that = (JsonSkillLevel) o;

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
