package api.simplified.hypixel.exception;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.client.exception.ApiErrorResponse;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HypixelErrorResponse implements ApiErrorResponse {

    @SerializedName("cause")
    protected String reason = "Unknown";
    protected boolean throttle;
    protected boolean global;

}
