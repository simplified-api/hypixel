package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Power;
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
    name = "powers"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonPower implements Power, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private @NotNull Optional<String> stoneId = Optional.empty();
    @SerializedName("requiredLevel")
    private int requiredCombatLevel = 0;
    private @NotNull Stage stage = Stage.STARTER;
    private @NotNull ConcurrentMap<String, Double> baseValues = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Double> bonuses = Concurrent.newMap();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonPower jsonPower = (JsonPower) o;

        return new EqualsBuilder()
            .append(this.getRequiredCombatLevel(), jsonPower.getRequiredCombatLevel())
            .append(this.getId(), jsonPower.getId())
            .append(this.getName(), jsonPower.getName())
            .append(this.getStoneId(), jsonPower.getStoneId())
            .append(this.getStage(), jsonPower.getStage())
            .append(this.getBaseValues(), jsonPower.getBaseValues())
            .append(this.getBonuses(), jsonPower.getBonuses())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.getStoneId())
            .append(this.getRequiredCombatLevel())
            .append(this.getStage())
            .append(this.getBaseValues())
            .append(this.getBonuses())
            .build();
    }

}
