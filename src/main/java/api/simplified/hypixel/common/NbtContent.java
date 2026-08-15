package api.simplified.hypixel.common;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.AllArgsConstructor;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.util.StringUtil;
import lib.minecraft.nbt.NbtFactory;
import lib.minecraft.nbt.exception.NbtException;
import lib.minecraft.nbt.tag.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * A container of items carried on the wire as base64 text rather than as JSON.
 * <p>
 * Every inventory, backpack and item bag arrives this way: the item stacks are gzipped NBT, base64
 * encoded, and handed over as one opaque string. Nothing here is readable until {@link #getNbtData()}
 * decodes it, so the raw string is what binds and the decode is deferred to the caller that needs it.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NbtContent {

    @Getter(AccessLevel.NONE)
    private int type = 0; // Always 0

    /**
     * The base64 text exactly as the wire sent it, empty when the container was never opened.
     */
    @SerializedName("data")
    private @NotNull String rawData = "";

    /**
     * The base64 text decoded to bytes, still gzipped.
     */
    public byte[] getData() {
        return StringUtil.decodeBase64(this.getRawData().toCharArray());
    }

    /**
     * The item stacks, decoded and inflated into a tag tree.
     *
     * @throws NbtException if the text is not valid gzipped NBT
     */
    public @NotNull CompoundTag getNbtData() throws NbtException {
        return NbtFactory.fromBase64(this.getRawData());
    }

    /**
     * Binds the two shapes the wire uses for a container, and writes back only the fuller one.
     * <p>
     * Hypixel sends either an object carrying {@code type} and {@code data} or, in a handful of
     * places, the base64 string on its own. Both read; the object form is what is written, so a
     * decoded container round-trips to the shape the rest of the wire uses.
     */
    public static class Adapter extends TypeAdapter<NbtContent> {

        /** {@inheritDoc} */
        @Override
        public void write(@NotNull JsonWriter out, @NotNull NbtContent value) throws IOException {
            out.beginObject()
                .name("type")
                .value(0)
                .name("data")
                .value(value.getRawData())
                .endObject();
        }

        /** {@inheritDoc} */
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
