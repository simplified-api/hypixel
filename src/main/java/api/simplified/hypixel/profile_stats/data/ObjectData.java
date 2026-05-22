package api.simplified.hypixel.profile_stats.data;

import dev.sbs.skyblockdata.SkyBlockData;
import dev.sbs.skyblockdata.common.Rarity;
import dev.sbs.skyblockdata.model.BonusItemStat;
import dev.sbs.skyblockdata.model.Gemstone;
import dev.sbs.skyblockdata.model.Item;
import dev.sbs.skyblockdata.model.Reforge;
import dev.sbs.skyblockdata.model.Stat;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.util.StringUtil;
import lib.minecraft.nbt.tag.Tag;
import lib.minecraft.nbt.tag.CompoundTag;
import lib.minecraft.nbt.tag.IntTag;
import lib.minecraft.nbt.tag.StringTag;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class ObjectData<T extends ObjectData.Type> extends StatData<T> {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("M/d/yy h:m a", Locale.US);
    private static final ZoneId HYPIXEL_TIMEZONE = ZoneId.of("America/New_York");
    @Getter private final Item item;
    @Getter private final CompoundTag compoundTag;
    @Getter private final Rarity rarity;
    @Getter private final ConcurrentList<BonusItemStat> bonusItemStatModels;
    @Getter private final Optional<Reforge> reforge;
    @Getter private final ConcurrentMap<Gemstone, ConcurrentList<Gemstone.Type>> gemstones;
    @Getter private final boolean recombobulated;
    @Getter private final boolean tierBoosted;
    @Getter private final Optional<Long> timestamp;

    protected ObjectData(Item itemModel, CompoundTag compoundTag) {
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
        ConcurrentList<Stat> statModels = SkyBlockData.getRepository(Stat.class).findAll();
        Arrays.stream(this.getAllTypes()).forEach(type -> {
            this.stats.put(type, Concurrent.newLinkedMap());
            statModels.forEach(statModel -> this.stats.get(type).put(statModel, new Data()));
        });

        // Load Bonus Item Stat Model
        this.bonusItemStatModels = SkyBlockData.getRepository(BonusItemStat.class)
            .findAll(BonusItemStat::getItemId, itemModel.getId())
            .collect(Concurrent.toUnmodifiableList());

        // Load Reforge Model
        this.reforge = SkyBlockData.getRepository(Reforge.class)
            .findFirst(Reforge::getId, this.getCompoundTag()
                .getPathOrDefault("tag.ExtraAttributes.modifier", StringTag.EMPTY)
                .getValue()
                .toUpperCase()
            );

        // Load Rarity
        this.rarity = Rarity.of(this.handleRarityUpgrades(
            itemModel.getRarity().ordinal() +
                (this.isRecombobulated() ? 1 : 0) +
                (this.isTierBoosted() ? 1 : 0)
        ));
    }

    public abstract ObjectData<T> calculateBonus(ConcurrentMap<String, Double> expressionVariables);

    private static ConcurrentMap<Gemstone, ConcurrentList<Gemstone.Type>> findGemstones(CompoundTag gemTag) {
        ConcurrentList<Gemstone> gemstoneModels = SkyBlockData.getRepository(Gemstone.class).findAll();
        ConcurrentMap<Gemstone, ConcurrentList<Gemstone.Type>> gemstones = Concurrent.newMap();

        for (Map.Entry<String, Tag<?>> entry : gemTag.entrySet()) {
            for (Gemstone gemstone : gemstoneModels) {
                boolean handle = false;
                String typeKey = null;

                // Handle Specific Slots
                if (entry.getKey().startsWith(gemstone.getId())) {
                    handle = true;
                    typeKey = ((StringTag) entry.getValue()).getValue();
                }

                // Handle Generic Slots
                if (entry.getValue().getValue().equals(gemstone.getId()) && entry.getKey().endsWith("_gem")) {
                    handle = true;
                    typeKey = gemTag.getOrDefault(entry.getKey().replace("_gem", ""), StringTag.EMPTY).getValue();
                }

                if (handle && typeKey != null) {
                    // Populate New Gemstone
                    gemstones.putIfAbsent(gemstone, Concurrent.newList());

                    // Add Gemstone Type
                    try {
                        Gemstone.Type gemType = Gemstone.Type.valueOf(typeKey);
                        gemstones.get(gemstone).add(gemType);
                    } catch (IllegalArgumentException ignore) { }
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

        return this.isRecombobulated() == that.isRecombobulated()
            && this.isTierBoosted() == that.isTierBoosted()
            && Objects.equals(this.getItem(), that.getItem())
            && Objects.equals(this.getCompoundTag(), that.getCompoundTag())
            && Objects.equals(this.getRarity(), that.getRarity())
            && Objects.equals(this.getBonusItemStatModels(), that.getBonusItemStatModels())
            && Objects.equals(this.getReforge(), that.getReforge())
            && Objects.equals(this.getGemstones(), that.getGemstones())
            && Objects.equals(this.getTimestamp(), that.getTimestamp());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getItem(), this.getCompoundTag(), this.getRarity(), this.getBonusItemStatModels(), this.getReforge(), this.getGemstones(), this.isRecombobulated(), this.isTierBoosted(), this.getTimestamp());
    }

    public interface Type {

        boolean isOptimizerConstant();

    }

}
