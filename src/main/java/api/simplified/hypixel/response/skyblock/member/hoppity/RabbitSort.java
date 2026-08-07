package api.simplified.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;

public enum RabbitSort {

    @SerializedName("a_to_z")
    A_TO_Z,
    @SerializedName("z_to_a")
    Z_TO_A,
    @SerializedName(value = "rarity_high_low", alternate = "highest_rarity")
    HIGHEST_RARITY,
    @SerializedName(value = "lowest_rarity", alternate = "rarity_low_high")
    LOWEST_RARITY

}
