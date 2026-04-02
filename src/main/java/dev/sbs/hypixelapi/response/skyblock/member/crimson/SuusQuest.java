package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SuusQuest {

    @SerializedName("talked_to_npc")
    private boolean talkedToNpc;
    @SerializedName("last_toy_drop")
    private long lastToyDrop;
    @SerializedName("last_completion")
    private long lastCompletion;

}
