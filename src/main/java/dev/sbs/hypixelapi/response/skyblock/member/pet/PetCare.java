package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.pet;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class PetCare {

    @SerializedName("coins_spent")
    public double coinsSpent;
    @SerializedName("pet_types_sacrificed")
    private @NotNull ConcurrentList<String> sacrificedPets = Concurrent.newList();

}