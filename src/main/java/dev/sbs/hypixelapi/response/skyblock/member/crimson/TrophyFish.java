package dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson;

import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.persistence.model.Zone;
import dev.sbs.minecraftapi.skyblock.common.Rarity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum TrophyFish {

    BLOBFISH("Blobfish", Rarity.COMMON),
    GUSHER("Gusher", Rarity.COMMON, "BLAZING_VOLCANO"),
    OBFUSCATED_FISH_1("Obfuscated 1", Rarity.COMMON),
    STEAMING_HOT_FLOUNDER("Steaming-Hot Flounder", Rarity.COMMON, "BLAZING_VOLCANO"),
    SULPHUR_SKITTER("Sulphur Skitter", Rarity.COMMON),
    FLYFISH("Flyfish", Rarity.UNCOMMON),
    OBFUSCATED_FISH_2("Obfuscated 2", Rarity.UNCOMMON),
    SLUGFISH("Slugfish", Rarity.UNCOMMON),
    LAVA_HORSE("Lava Horse", Rarity.RARE),
    MANA_RAY("Mana Ray", Rarity.RARE),
    OBFUSCATED_FISH_3("Obfuscated 3", Rarity.RARE),
    VANILLE("Vanille", Rarity.RARE),
    VOLCANIC_STONEFISH("Volcanic Stonefish", Rarity.RARE, "BLAZING_VOLCANO"),
    KARATE_FISH("Karate Fish", Rarity.EPIC, "DOJO"),
    MOLDFIN("Moldfin", Rarity.EPIC, "MYSTIC_MARSH"),
    SKELETON_FISH("Skeleton Fish", Rarity.EPIC, "BURNING_DESERT"),
    SOUL_FISH("Soul Fish", Rarity.EPIC, "STRONGHOLD"),
    GOLDEN_FISH("Golden Fish", Rarity.LEGENDARY);

    private final @NotNull String displayName;
    private final @NotNull Rarity rarity;
    private final @NotNull Optional<String> zoneId;

    TrophyFish(@NotNull String displayName, @NotNull Rarity rarity) {
        this(displayName, rarity, Optional.empty());
    }

    TrophyFish(@NotNull String displayName, @NotNull Rarity rarity, @NotNull String zoneId) {
        this(displayName, rarity, Optional.of(zoneId));
    }

    public @NotNull Optional<Zone> getZone() {
        return this.getZoneId().map(zoneId -> MinecraftApi.getRepository(Zone.class).findFirstOrNull(Zone::getId, zoneId));
    }

    public enum Tier {

        BRONZE,
        SILVER,
        GOLD,
        DIAMOND

    }

}
