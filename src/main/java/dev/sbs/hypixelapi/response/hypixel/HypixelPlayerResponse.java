package dev.sbs.minecraftapi.client.hypixel.response.hypixel;

import dev.sbs.minecraftapi.hypixel.HypixelPlayer;
import lombok.Getter;

import java.util.Optional;

@Getter
public class HypixelPlayerResponse {

    private boolean success;
    private Optional<HypixelPlayer> player = Optional.empty();

}
