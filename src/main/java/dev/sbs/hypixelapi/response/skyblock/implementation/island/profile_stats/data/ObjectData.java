package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.profile_stats.data;

import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.builder.EqualsBuilder;
import dev.sbs.api.builder.HashCodeBuilder;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.minecraftapi.data.model.bonus_data.bonus_item_stats.BonusItemStatModel;
import dev.sbs.minecraftapi.data.model.gemstone_data.gemstone_types.GemstoneTypeModel;
import dev.sbs.minecraftapi.data.model.gemstone_data.gemstones.GemstoneModel;
import dev.sbs.minecraftapi.data.model.items.ItemModel;
import dev.sbs.minecraftapi.data.model.rarities.RarityModel;
import dev.sbs.minecraftapi.data.model.reforge_data.reforges.ReforgeModel;
import dev.sbs.minecraftapi.data.model.stats.StatModel;
import dev.sbs.api.util.StringUtil;
import dev.sbs.minecraftapi.nbt.tags.Tag;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.IntTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public abstract class ObjectData<T extends ObjectData.Type> extends StatData<T> {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("M/d/yy h:m a", Locale.US);
    private static final ZoneId HYPIXEL_TIMEZONE = ZoneId.of("America/New_York");
    @Getter private final ItemModel item;
    @Getter private final CompoundTag compoundTag;
    @Getter private final RarityModel rarity;
    @Getter private final ConcurrentList<BonusItemStatModel> bonusItemStatModels;
    @Getter private final Optional<ReforgeModel> reforge;
    @Getter private final ConcurrentMap<GemstoneModel, ConcurrentList<GemstoneTypeModel>> gemstones;
    @Getter private final boolean recombobulated;
    @Getter private final boolean tierBoosted;
    @Getter private final Optional<Long> timestamp;

    protected ObjectData(ItemModel itemModel, CompoundTag compoundTag) {
        this.item = itemModel;
        this.compoundTag = compoundTag;

        // Load Timestamp
        this.timestamp = Optional.ofNullable(
                StringUtil.defaultIfEmpty(
                    compoundTag.getPathOrDefault("tag.ExtraAttributes.timestamp", StringTag.EMPTY).getValue(),
                    null
                )
            )
            .map(timestamp -> LocalDateTime.parse(timestamp, TIMESTAMP_FORMAT))
            .map(localDateTime -> localDateTime.atZone(HYPIXEL_TIMEZONE))
            .map(ZonedDateTime::toInstant)
            .map(Instant::toEpochMilli);

        // Load Recombobulator
        this.recombobulated = compoundTag.getPathOrDefault("tag.ExtraAttributes.rarity_upgrades", IntTag.EMPTY).getValue() == 1;

        // Load Tier Boost
        this.tierBoosted = compoundTag.getPathOrDefault("tag.ExtraAttributes.baseStatBoostPercentage", IntTag.EMPTY).getValue() > 0;

        // Load Gemstones
        CompoundTag gemTag = compoundTag.getPathOrDefault("tag.ExtraAttributes.gems", CompoundTag.EMPTY);
        this.gemstones = Concurrent.newUnmodifiableMap(gemTag.notEmpty() ? findGemstones(gemTag) : Concurrent.newMap());

        // Initialize Stats
        ConcurrentList<StatModel> statModels = SimplifiedApi.getRepositoryOf(StatModel.class).findAll().sorted(StatModel::getOrdinal);
        Arrays.stream(this.getAllTypes()).forEach(type -> {
            this.stats.put(type, Concurrent.newLinkedMap());
            statModels.forEach(statModel -> this.stats.get(type).put(statModel, new Data()));
        });

        // Load Bonus Item Stat Model
        this.bonusItemStatModels = SimplifiedApi.getRepositoryOf(BonusItemStatModel.class)
            .findAll(BonusItemStatModel::getItem, itemModel)
            .collect(Concurrent.toUnmodifiableList());

        // Load Reforge Model
        this.reforge = SimplifiedApi.getRepositoryOf(ReforgeModel.class)
            .findFirst(ReforgeModel::getKey, this.getCompoundTag()
                .getPathOrDefault("tag.ExtraAttributes.modifier", StringTag.EMPTY)
                .getValue()
                .toUpperCase()
            );

        // Load Rarity
        this.rarity = SimplifiedApi.getRepositoryOf(RarityModel.class)
            .findFirst(
                RarityModel::getOrdinal,
                this.handleRarityUpgrades(
                    itemModel.getRarity().getOrdinal() +
                        (this.isRecombobulated() ? 1 : 0) +
                        (this.isTierBoosted() ? 1 : 0)
                )
            )
            .orElse(itemModel.getRarity());
    }

    public abstract ObjectData<T> calculateBonus(ConcurrentMap<String, Double> expressionVariables);

    private static ConcurrentMap<GemstoneModel, ConcurrentList<GemstoneTypeModel>> findGemstones(CompoundTag gemTag) {
        ConcurrentList<GemstoneModel> gemstoneModels = SimplifiedApi.getRepositoryOf(GemstoneModel.class).findAll();
        ConcurrentList<GemstoneTypeModel> gemstoneTypeModels = SimplifiedApi.getRepositoryOf(GemstoneTypeModel.class).findAll();
        ConcurrentMap<GemstoneModel, ConcurrentList<GemstoneTypeModel>> gemstones = Concurrent.newMap();

        for (Map.Entry<String, Tag<?>> entry : gemTag.entrySet()) {
            for (GemstoneModel gemstoneModel : gemstoneModels) {
                boolean handle = false;
                String typeKey = null;

                // Handle Specific Slots
                if (entry.getKey().startsWith(gemstoneModel.getKey())) {
                    handle = true;
                    typeKey = ((StringTag) entry.getValue()).getValue();
                }

                // Handle Generic Slots
                if (entry.getValue().getValue().equals(gemstoneModel.getKey()) && entry.getKey().endsWith("_gem")) {
                    handle = true;
                    typeKey = gemTag.getOrDefault(entry.getKey().replace("_gem", ""), StringTag.EMPTY).getValue();
                }

                if (handle) {
                    // Populate New Gemstone
                    gemstones.putIfAbsent(gemstoneModel, Concurrent.newList());

                    // Add Gemstone Type
                    gemstoneTypeModels.findFirst(GemstoneTypeModel::getKey, typeKey)
                        .ifPresent(gemstoneTypeModel -> gemstones.get(gemstoneModel).add(gemstoneTypeModel));
                }
            }
        }

        return gemstones;
    }

    protected int handleRarityUpgrades(int rarityOrdinal) {
        return rarityOrdinal;
    }

    public abstract boolean isBonusCalculated();

    public final boolean notRecombobulated() {
        return !this.isRecombobulated();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectData<?> that = (ObjectData<?>) o;

        return new EqualsBuilder()
            .append(this.isRecombobulated(), that.isRecombobulated())
            .append(this.isTierBoosted(), that.isTierBoosted())
            .append(this.getItem(), that.getItem())
            .append(this.getCompoundTag(), that.getCompoundTag())
            .append(this.getRarity(), that.getRarity())
            .append(this.getBonusItemStatModels(), that.getBonusItemStatModels())
            .append(this.getReforge(), that.getReforge())
            .append(this.getGemstones(), that.getGemstones())
            .append(this.getTimestamp(), that.getTimestamp())
            .build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(this.getItem())
            .append(this.getCompoundTag())
            .append(this.getRarity())
            .append(this.getBonusItemStatModels())
            .append(this.getReforge())
            .append(this.getGemstones())
            .append(this.isRecombobulated())
            .append(this.isTierBoosted())
            .append(this.getTimestamp())
            .build();
    }

    public interface Type {

        boolean isOptimizerConstant();

    }

}
