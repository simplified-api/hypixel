package dev.sbs.minecraftapi.client.hypixel.response.hypixel;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class HypixelGuildResponse {

    private boolean success;
    private @NotNull Optional<HypixelGuild> guild = Optional.empty();

}
