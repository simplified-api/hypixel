package api.simplified.hypixel.response.skyblock.member.mining;

import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.SkyBlockData;
import dev.sbs.skyblockdata.date.SkyBlockDate;
import dev.sbs.skyblockdata.model.Item;
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