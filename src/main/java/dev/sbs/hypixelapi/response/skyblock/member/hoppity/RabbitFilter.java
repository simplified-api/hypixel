package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;

public enum RabbitFilter {

    NONE,
    @SerializedName("found")
    FOUND,
    @SerializedName("not_found")
    NOT_FOUND,
    @SerializedName("achievements")
    HAS_REQUIREMENT,
    @SerializedName("non_achievements")
    NO_REQUIREMENT

}
