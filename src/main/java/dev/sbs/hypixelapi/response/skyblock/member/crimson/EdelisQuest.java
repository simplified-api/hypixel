package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@NoArgsConstructor
public class EdelisQuest {

    @Accessors(fluent = true)
    @SerializedName("heard_story_statue")
    private boolean hasHeardStoryStatue;

}
