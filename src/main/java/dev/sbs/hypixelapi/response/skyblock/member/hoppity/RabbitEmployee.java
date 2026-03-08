package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum RabbitEmployee {

    @SerializedName("RABBIT_BRO")
    BRO("Bro"),
    @SerializedName("RABBIT_COUSIN")
    COUSIN("Cousin"),
    @SerializedName("RABBIT_SIS")
    SISTER("Sis"),
    @SerializedName("RABBIT_FATHER")
    FATHER("Daddy"),
    @SerializedName("RABBIT_GRANDMA")
    GRANDMA("Granny"),
    @SerializedName("RABBIT_UNCLE")
    UNCLE("Uncle"),
    @SerializedName("RABBIT_DOG")
    DOG("Dog");

    private final @NotNull String name;

}
