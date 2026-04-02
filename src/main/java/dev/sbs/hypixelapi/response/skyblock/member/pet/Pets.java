package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.pet;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.util.StreamUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class Pets {

    private final static @NotNull ConcurrentList<Integer> magicFindPetScore = Concurrent.newList(10, 25, 50, 75, 100, 130, 175, 225, 275, 325, 375, 450, 500);
    private @NotNull ConcurrentList<OwnedPet> pets = Concurrent.newList();
    @SerializedName("pet_care")
    private @NotNull PetCare petCare = new PetCare();
    private @NotNull AutoPet autopet = new AutoPet();

    public @NotNull Optional<OwnedPet> getActivePet() {
        return this.getPets()
            .stream()
            .filter(OwnedPet::isActive)
            .findFirst();
    }

    public int getPetScore() {
        return this.getPets()
            .sorted(OwnedPet::getRarity)
            .stream()
            .filter(StreamUtil.distinctByKey(OwnedPet::getId))
            .mapToInt(OwnedPet::getScore)
            .sum();
    }

    public int getPetScoreMagicFind() {
        return magicFindPetScore.matchAll(breakpoint -> breakpoint <= this.getPetScore())
            .reduce((m1, m2) -> m2)
            .orElse(0);
    }

}
