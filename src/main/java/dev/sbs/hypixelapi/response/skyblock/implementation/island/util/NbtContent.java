package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.nbt.exception.NbtException;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class NbtContent {

    private int type; // Always 0
    @SerializedName("data")
    @Getter private String rawData;

    public byte[] getData() {
        return StringUtil.decodeBase64(this.getRawData().toCharArray());
    }

    public @NotNull CompoundTag getNbtData() throws NbtException {
        return MinecraftApi.getNbtFactory().fromBase64(this.getRawData());
    }

}