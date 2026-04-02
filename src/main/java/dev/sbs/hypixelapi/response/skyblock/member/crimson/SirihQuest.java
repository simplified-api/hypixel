package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SirihQuest {

    @SerializedName("sulphur_given")
    private int sulphurGiven;
    @SerializedName("last_give")
    private long lastGive;
    @SerializedName("dialogue_index")
    private int dialogueIndex;

}
