package dev.sbs.minecraftapi.client.hypixel.exception;

import dev.sbs.api.client.exception.ClientException;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public final class HypixelClientException extends ClientException {

    private final @NotNull HypixelErrorResponse response;

    public HypixelClientException(@NotNull String methodKey, @NotNull feign.Response response) {
        super(methodKey, response, "Hypixel");
        this.response = this.getBody()
            .map(json -> super.fromJson(json, HypixelErrorResponse.class))
            .orElse(new HypixelErrorResponse.Unknown());
    }

}
