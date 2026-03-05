package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.MelodySong;
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
    name = "melody_songs"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonMelodySong implements MelodySong, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Difficulty difficulty = Difficulty.EASY;
    private int intelligenceReward;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonMelodySong that = (JsonMelodySong) o;

        return new EqualsBuilder()
            .append(this.getIntelligenceReward(), that.getIntelligenceReward())
            .append(this.getId(), that.getId())
            .append(this.getName(), that.getName())
            .append(this.getDifficulty(), that.getDifficulty())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getDifficulty())
            .append(this.getIntelligenceReward())
            .build();
    }

}
