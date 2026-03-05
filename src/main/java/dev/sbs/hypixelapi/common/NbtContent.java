package dev.sbs.minecraftapi.skyblock.common;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.nbt.exception.NbtException;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NbtContent {

    @Getter(AccessLevel.NONE)
    private int type; // Always 0

    @SerializedName("data")
    private String rawData;

    public byte[] getData() {
        return StringUtil.decodeBase64(this.getRawData().toCharArray());
    }

    public @NotNull CompoundTag getNbtData() throws NbtException {
        return MinecraftApi.getNbtFactory().fromBase64(this.getRawData());
    }

}
