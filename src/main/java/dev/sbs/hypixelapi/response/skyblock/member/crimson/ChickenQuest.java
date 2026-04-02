package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor
public class ChickenQuest {

    @SerializedName("chicken_quest_start")
    private boolean started;
    @SerializedName("chicken_quest_progress")
    private int progress;
    @SerializedName("chicken_quest_collected")
    private @NotNull ConcurrentList<String> collected = Concurrent.newList();

}
