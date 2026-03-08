package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;

public enum RabbitFilter {

    NONE,
    FOUND,
    NOT_FOUND,
    @SerializedName("ACHIEVEMENTS")
    HAS_REQUIREMENT,
    @SerializedName("NON_ACHIEVEMENTS")
    NO_REQUIREMENT

}
