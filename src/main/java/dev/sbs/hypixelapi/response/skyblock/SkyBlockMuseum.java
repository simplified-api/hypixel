package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.skyblock.common.NbtContent;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public class SkyBlockMuseum {

    private long value;
    private boolean appraisal;
    private @NotNull ConcurrentMap<String, Item> items = Concurrent.newMap();
    @SerializedName("special")
    private @NotNull ConcurrentList<Item> specialItems = Concurrent.newList();

    @Getter
    public static class Item {

        @SerializedName("donated_time")
        private SkyBlockDate.RealTime donated;
        private boolean borrowing;
        @SerializedName("featured_slot")
        private @NotNull Optional<String> featuredSlot = Optional.empty();
        private @NotNull NbtContent items = new NbtContent();

        public boolean isFeatured() {
            return this.featuredSlot.isPresent();
        }

        public boolean notFeatured() {
            return !this.isFeatured();
        }

    }

}
