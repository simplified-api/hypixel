package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.hoppity;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum RabbitEmployee {

    @SerializedName("rabbit_bro")
    BRO("Bro"),
    @SerializedName("rabbit_cousin")
    COUSIN("Cousin"),
    @SerializedName("rabbit_sis")
    SISTER("Sis"),
    @SerializedName("rabbit_father")
    FATHER("Daddy"),
    @SerializedName("rabbit_grandma")
    GRANDMA("Granny"),
    @SerializedName("rabbit_uncle")
    UNCLE("Uncle"),
    @SerializedName("rabbit_dog")
    DOG("Dog");

    private final @NotNull String name;

}
