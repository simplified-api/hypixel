package dev.sbs.minecraftapi.client.hypixel.response.hypixel.player;

import lombok.Getter;

import java.util.Optional;

@Getter
public class HypixelPlayerResponse {

    private boolean success;
    private Optional<HypixelPlayer> player = Optional.empty();

}
