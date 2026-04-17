package dev.sbs.minecraftapi.skyblock.common;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dev.sbs.minecraftapi.MinecraftApi;
import lib.minecraft.nbt.exception.NbtException;
import lib.minecraft.nbt.tags.collection.CompoundTag;
import dev.simplified.util.StringUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NbtContent {

    @Getter(AccessLevel.NONE)
    private int type = 0; // Always 0

    @SerializedName("data")
    private @NotNull String rawData = "";

    public byte[] getData() {
        return StringUtil.decodeBase64(this.getRawData().toCharArray());
    }

    public @NotNull CompoundTag getNbtData() throws NbtException {
        return MinecraftApi.getNbtFactory().fromBase64(this.getRawData());
    }

    public static class Adapter extends TypeAdapter<NbtContent> {

        @Override
        public void write(@NotNull JsonWriter out, @NotNull NbtContent value) throws IOException {
            out.beginObject()
                .name("type")
                .value(0)
                .name("data")
                .value(value.getRawData())
                .endObject();
        }

        @Override
        public NbtContent read(@NotNull JsonReader in) throws IOException {
            String data = "";

            if (in.peek() == JsonToken.BEGIN_OBJECT) {
                in.beginObject();

                while (in.hasNext()) {
                    String name = in.nextName();

                    if ("data".equals(name))
                        data = in.nextString();
                    else
                        in.skipValue();
                }

                in.endObject();
            } else
                data = in.nextString();

            return new NbtContent(0, data);
        }

    }

}
