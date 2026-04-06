package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.NoArgsConstructor;

public enum Powder {

    MITHRIL,
    GEMSTONE,
    GLACITE;

    @Getter
    @NoArgsConstructor
    public static class Data {

        @SerializedName("")
        private int amount;
        private int total;
        @SerializedName("spent_")
        private int spent;

    }

}
