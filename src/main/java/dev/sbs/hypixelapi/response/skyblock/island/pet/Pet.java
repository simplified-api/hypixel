package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.pet;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.client.hypixel.response.Rarity;
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
