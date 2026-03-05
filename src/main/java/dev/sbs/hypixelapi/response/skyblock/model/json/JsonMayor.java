package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.json;

import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.json.JsonModel;
import dev.sbs.api.persistence.json.JsonResource;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.model.Mayor;
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
    name = "mayors"
)
@NoArgsConstructor(access = AccessLevel.NONE)
public class JsonMayor implements Mayor, JsonModel {

    private @Id @NotNull String id = "";
    private @NotNull String name = "";
    private boolean special;
    private @NotNull ChatFormat format = ChatFormat.LIGHT_PURPLE;
    private @NotNull ConcurrentList<JsonPerk> perks = Concurrent.newList();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        JsonMayor jsonMayor = (JsonMayor) o;

        return new EqualsBuilder()
            .append(this.isSpecial(), jsonMayor.isSpecial())
            .append(this.getId(), jsonMayor.getId())
            .append(this.getName(), jsonMayor.getName())
            .append(this.getFormat(), jsonMayor.getFormat())
            .append(this.getPerks(), jsonMayor.getPerks())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getId())
            .append(this.getName())
            .append(this.isSpecial())
            .append(this.getFormat())
            .append(this.getPerks())
            .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.NONE)
    public static class JsonPerk implements Perk {

        private @NotNull String id = "";
        private @NotNull String name = "";
        private @NotNull String description = "";
        private @NotNull ConcurrentList<JsonSubstitute> stats = Concurrent.newList();

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonPerk jsonPerk = (JsonPerk) o;

            return new EqualsBuilder()
                .append(this.getId(), jsonPerk.getId())
                .append(this.getName(), jsonPerk.getName())
                .append(this.getDescription(), jsonPerk.getDescription())
                .append(this.getStats(), jsonPerk.getStats())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getName())
                .append(this.getDescription())
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
        private double value;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            JsonSubstitute that = (JsonSubstitute) o;

            return new EqualsBuilder()
                .append(this.getPrecision(), that.getPrecision())
                .append(this.getValue(), that.getValue())
                .append(this.getId(), that.getId())
                .append(this.getType(), that.getType())
                .append(this.getFormat(), that.getFormat())
                .build();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(this.getId())
                .append(this.getPrecision())
                .append(this.getType())
                .append(this.getFormat())
                .append(this.getValue())
                .build();
        }

    }

}
