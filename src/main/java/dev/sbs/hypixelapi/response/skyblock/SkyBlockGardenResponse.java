package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor
public class SkyBlockGardenResponse {

    private boolean success;
    private @NotNull SkyBlockGarden garden = new SkyBlockGarden();

}
