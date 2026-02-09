package dev.sbs.minecraftapi.client.hypixel.exception;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.client.exception.ApiErrorResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HypixelErrorResponse implements ApiErrorResponse {

    @SerializedName("cause")
    protected String reason;
    protected boolean throttle;
    protected boolean global;

    public static class Unknown extends HypixelErrorResponse {

        public Unknown() {
            super.reason = "Unknown";
        }

    }

}
