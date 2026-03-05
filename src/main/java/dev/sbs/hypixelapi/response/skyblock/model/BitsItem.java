package dev.sbs.minecraftapi.client.hypixel.response.skyblock.model;

import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.persistence.Model;
import dev.sbs.api.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public interface BitsItem extends Model {

    @NotNull String getId();

    @NotNull Type getType();

    int getCost();

    @NotNull ConcurrentList<Variant> getVariants();

    default boolean hasVariants() {
        return !this.getVariants().isEmpty();
    }

    default boolean noVariants() {
        return !this.hasVariants();
    }

    enum Type {

        ACCESSORY,
        ATTRIBUTE_SHARD,
        CONSUMABLE,
        COSMETIC,
        DYE,
        ENCHANTED_BOOK,
        ENRICHMENT,
        ITEM,
        SACK,
        UPGRADE;

        public @NotNull String getName() {
            return StringUtil.capitalizeFully(this.name().replace("_", " "));
        }

    }

    interface Variant {

        @NotNull String getId();

        int getCost();

    }

}
