package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island.pet;

import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.stream.StreamUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class PetData {

    private final static @NotNull ConcurrentList<Integer> magicFindPetScore = Concurrent.newList(10, 25, 50, 75, 100, 130, 175, 225, 275, 325, 375, 450, 500);
    private final @NotNull ConcurrentList<Pet> pets = Concurrent.newList();
    private final AutoPet autopet = new AutoPet();

    public @NotNull Optional<Pet> getActivePet() {
        return this.getPets()
            .stream()
            .filter(Pet::isActive)
            .findFirst();
    }

    public int getPetScore() {
        return this.getPets()
            .sorted(Pet::getRarity)
            .stream()
            .filter(StreamUtil.distinctByKey(Pet::getType))
            .mapToInt(Pet::getScore)
            .sum();
    }

    public int getPetScoreMagicFind() {
        return magicFindPetScore.matchAll(breakpoint -> breakpoint <= this.getPetScore())
            .reduce((m1, m2) -> m2)
            .orElse(0);
    }

}
