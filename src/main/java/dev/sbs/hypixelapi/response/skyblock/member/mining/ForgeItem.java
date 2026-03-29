package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.persistence.model.Item;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ForgeItem {

    private String type;
    @SerializedName("id")
    private String itemId;
    @SerializedName("startTime")
    private SkyBlockDate.RealTime started;
    private int slot;
    private boolean notified;

    public @NotNull Item getItem() {
        return MinecraftApi.getRepository(Item.class).findFirstOrNull(Item::getId, this.getItemId());
    }

}