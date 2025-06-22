package dev.sbs.minecraftapi.client.hypixel.response.hypixel.implementation;

import lombok.Getter;

import java.util.Optional;

@Getter
public class HypixelSession {

    private boolean online;
    private Optional<String> gameType = Optional.empty();
    private Optional<String> mode = Optional.empty();

}
