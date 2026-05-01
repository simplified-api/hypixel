package api.simplified.hypixel.exception;

import com.google.gson.annotations.SerializedName;
import dev.simplified.client.exception.ApiErrorResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HypixelErrorResponse implements ApiErrorResponse {

    @SerializedName("cause")
    protected String reason = "Unknown";
    protected boolean throttle;
    protected boolean global;

}
