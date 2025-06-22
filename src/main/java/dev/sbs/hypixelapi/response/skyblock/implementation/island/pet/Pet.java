package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.pet;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.client.hypixel.response.Rarity;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.Experience;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.weight.Weight;
import dev.sbs.api.util.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Pet {

    @SerializedName("uuid")
    protected UUID identifier;
    @SerializedName("uniqueId")
    protected UUID uniqueId;
    protected String type;
    @SerializedName("exp")
    protected double experience;
    protected boolean active;
    @SerializedName("tier")
    protected Rarity baseRarity;
    protected int candyUsed;
    protected Optional<String> heldItem = Optional.empty();
    protected Optional<String> skin = Optional.empty();

    /**
     * Wraps this class in a {@link Experience} and {@link Weight} class.
     * <br><br>
     * Requires an active database session.
     */
    public @NotNull EnhancedPet asEnhanced() {
        return new EnhancedPet(this);
    }

    public String getPrettyName() {
        return StringUtil.capitalizeFully(this.getType().replace("_", " "));
    }

    public @NotNull Rarity getRarity() {
        return Rarity.of(this.getBaseRarity().ordinal() + (this.isTierBoosted() ? 1 : 0));
    }

    public int getRarityOrdinal() {
        return this.getRarity().ordinal();
    }

    public int getScore() {
        return this.getRarity().ordinal() + 1;
    }

    public boolean isTierBoosted() {
        return this.getHeldItem().map(heldItem -> heldItem.equals("PET_ITEM_TIER_BOOST")).orElse(false);
    }

}
