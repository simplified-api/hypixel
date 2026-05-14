package api.simplified.hypixel.exception;

import com.google.gson.Gson;
import dev.simplified.client.exception.ErrorContext;
import dev.simplified.client.exception.JsonApiException;
import org.jetbrains.annotations.NotNull;

public final class HypixelApiException extends JsonApiException {

    public HypixelApiException(@NotNull Gson gson, @NotNull ErrorContext context) {
        super(context, "Hypixel");
        this.resolve(gson, HypixelErrorResponse.class);
    }

    @Override
    public @NotNull HypixelErrorResponse getResponse() {
        return (HypixelErrorResponse) super.getResponse();
    }

}
