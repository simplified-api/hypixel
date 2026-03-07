package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public class WizardTower {

    @SerializedName("wizard_quest_step")
    private int wizardQuestStep;
    @SerializedName("crumbs_laid_out")
    private int crumbsLaidOut;

}
