package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NpcQuest {

    @SerializedName("talked_to_npc")
    private boolean talkedToNpc;
    @SerializedName("last_completion")
    private long lastCompletion;

}
