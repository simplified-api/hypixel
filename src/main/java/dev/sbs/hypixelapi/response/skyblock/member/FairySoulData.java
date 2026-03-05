package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FairySoulData {

    @SerializedName("total_collected")
    private int totalCollected;
    @SerializedName("fairy_exchanges")
    private int exchanges;
    @SerializedName("unspent_souls")
    private int unspent;

}