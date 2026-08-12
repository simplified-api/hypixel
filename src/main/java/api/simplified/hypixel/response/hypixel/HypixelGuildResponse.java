package api.simplified.hypixel.response.hypixel;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Response envelope wrapping a guild lookup.
 */
@Getter
public class HypixelGuildResponse {

    /**
     * Whether the wire reported the request as successful.
     */
    private boolean success;

    /**
     * The guild that was matched, absent when the lookup matched no guild.
     */
    private @NotNull Optional<HypixelGuild> guild = Optional.empty();

}
