package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Getter
public class Dreadfarm {

    @SerializedName("shania_stage")
    private int shaniaStage;
    @SerializedName("caducous_feeder_uses")
    private @NotNull ConcurrentList<Instant> caducousFeederUses = Concurrent.newList();

}
