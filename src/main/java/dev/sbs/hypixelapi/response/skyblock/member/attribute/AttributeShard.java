package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.attribute;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.time.Instant;

@Getter
public class AttributeShard {

    private String type;
    @SerializedName("amount_owned")
    private int amount;
    private Instant captured;

}
