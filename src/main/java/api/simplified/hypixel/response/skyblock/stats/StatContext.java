package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.hypixel.response.skyblock.SkyBlockIsland;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * What one compute reads and never writes: the member being totalled and the island they are on.
 * <p>
 * Nothing a compute produces is in here. A stage that needs a value an earlier stage made is handed
 * it, so no stage can reach a half-built one through the back of this.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class StatContext {

    /**
     * The profile the member belongs to, read for the shared bank balance.
     */
    private final @NotNull SkyBlockIsland island;

    /**
     * The member being totalled.
     */
    private final @NotNull SkyBlockMember member;

}
