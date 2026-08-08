package api.simplified.hypixel.response.skyblock.member.mining;

import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.date.SkyBlockDate;
import api.simplified.skyblock.model.Item;
import com.google.gson.annotations.SerializedName;
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
        return SkyBlockData.getRepository(Item.class).findFirstOrNull(Item::getId, this.getItemId());
    }

}