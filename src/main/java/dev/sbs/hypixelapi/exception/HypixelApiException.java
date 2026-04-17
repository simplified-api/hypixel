package dev.sbs.minecraftapi.client.hypixel.exception;

import com.google.gson.Gson;
import dev.simplified.client.exception.JsonApiException;
import org.jetbrains.annotations.NotNull;

public final class HypixelApiException extends JsonApiException {

    public HypixelApiException(@NotNull Gson gson, @NotNull String methodKey, @NotNull feign.Response response) {
        super(methodKey, response, "Hypixel", gson, HypixelErrorResponse.class, HypixelErrorResponse.Unknown::new);
    }

    @Override
    public @NotNull HypixelErrorResponse getResponse() {
        return (HypixelErrorResponse) super.getResponse();
    }

}
