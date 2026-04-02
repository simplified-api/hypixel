package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AlchemistQuest {

    @SerializedName("alchemist_quest_start")
    private boolean started;
    @SerializedName("alchemist_quest_progress")
    private int progress;

}
