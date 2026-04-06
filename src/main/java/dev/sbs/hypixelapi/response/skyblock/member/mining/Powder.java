package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class Powder {

    @SerializedName("")
    private int amount;
    private int total;
    @SerializedName("spent_")
    private int spent;

    public enum Type {

        MITHRIL,
        GEMSTONE,
        GLACITE

    }

}
