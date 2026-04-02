package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.pet.OwnedPet;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class DeadCats {

    @Accessors(fluent = true)
    @SerializedName("talked_to_jacquelle")
    private boolean hasTalkedToJacquelle;
    @SerializedName("picked_up_detector")
    private boolean detectorPickedUp;
    @SerializedName("found_cats")
    private @NotNull ConcurrentList<String> foundCats = Concurrent.newList();
    @SerializedName("unlocked_pet")
    private boolean petUnlocked;
    private @NotNull Optional<OwnedPet> montezuma = Optional.empty();

}
