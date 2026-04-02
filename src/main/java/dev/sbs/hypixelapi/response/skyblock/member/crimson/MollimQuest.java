package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MollimQuest {

    @SerializedName("talked_to_npc")
    private boolean talkedToNpc;
    @SerializedName("completed_quest")
    private boolean completedQuest;

}
