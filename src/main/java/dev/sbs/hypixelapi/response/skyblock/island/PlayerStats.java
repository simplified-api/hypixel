package dev.sbs.minecraftapi.client.hypixel.response.skyblock.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.minecraftapi.client.hypixel.response.Rarity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Getter
public class PlayerStats {

    private Races races = new Races();
    private Mythos mythos = new Mythos();
    private Auctions auctions = new Auctions();
    @SerializedName("end_island")
    private EndIsland endIsland = new EndIsland();
    private Winter winter = new Winter();
    @SerializedName("sea_creature_kills")
    private int seaCreatureKills;
    @SerializedName("items_fished")
    private ItemsFished itemsFished = new ItemsFished();
    private Gifts gifts = new Gifts();
    @SerializedName("shredder_rod")
    private ShredderRod shredderRod = new ShredderRod();

    private @NotNull ConcurrentMap<String, Integer> kills = Concurrent.newMap();
    private @NotNull ConcurrentMap<String, Integer> deaths = Concurrent.newMap();
    @SerializedName("rift")
    private @NotNull ConcurrentMap<String, Object> riftStats = Concurrent.newMap();
    @SerializedPath("spooky.bats_spawned")
    private @NotNull ConcurrentMap<Integer, Integer> spawnedSpookyBats = Concurrent.newMap();

    // Damage
    @SerializedName("highest_damage")
    private double highestDamage;
    @SerializedName("highest_critical_damage")
    private double highestCriticalDamage;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Races {

        @SerializedName("dungeon_hub")
        private @NotNull ConcurrentMap<String, Integer> dungeonHub = Concurrent.newMap();
        @SerializedName("chicken_race_best_time_2")
        private int chickenRaceBestTime;
        @SerializedName("foraging_race_best_time")
        private int foragingRaceBestTime;
        @SerializedName("rift_race_best_time")
        private int riftRaceBestTime;
        @SerializedName("end_race_best_time")
        private int endRaceBestTime;

    }

    @Getter
    public static class Mythos {

        private int kills;
        @SerializedName("burrows_chains_complete")
        private @NotNull ConcurrentMap<Type, Integer> completedChains = Concurrent.newMap();
        @SerializedName("burrows_dug_treasure")
        private @NotNull ConcurrentMap<Type, Integer> dugTreasure = Concurrent.newMap();
        @SerializedName("burrows_dug_combat")
        private @NotNull ConcurrentMap<Type, Integer> dugCombat = Concurrent.newMap();
        @SerializedName("burrows_dug_next")
        private @NotNull ConcurrentMap<Type, Integer> dugNext = Concurrent.newMap();

        public enum Type {

            @SerializedName("null")
            UNKNOWN,
            COMMON,
            EPIC,
            LEGENDARY,
            TOTAL

        }

    }

    @Getter
    public static class Auctions {

        // Total
        @SerializedName("total_bought")
        private @NotNull ConcurrentMap<Rarity, Integer> totalBought = Concurrent.newMap();
        @SerializedName("total_sold")
        private @NotNull ConcurrentMap<Rarity, Integer> totalSold = Concurrent.newMap();
        @SerializedName("won")
        private int totalWon;
        @SerializedName("created")
        private int totalCreated;
        @SerializedName("completed")
        private int totalCompleted;

        // Gold
        @SerializedName("gold_spent")
        private long goldSpent;
        @SerializedName("gold_earned")
        private long goldEarned;
        @SerializedName("fees")
        private long goldFees;

        // Bids
        private int bids;
        @SerializedName("no_bids")
        private int noBids;
        @SerializedName("highest_bid")
        private long highestBid;

    }

    @Getter
    public static class EndIsland {

        @SerializedName("dragon_fight")
        private DragonFight dragonFight = new DragonFight();
        @SerializedName("summoning_eyes_collected")
        private int summoningEyesCollected;
        @SerializedName("special_zealot_loot_collected")
        private int specialZealotLootCollected;

        @Getter
        public static class DragonFight {

            @SerializedName("highest_rank")
            private @NotNull ConcurrentMap<Type, Integer> highestRank = Concurrent.newMap();
            @SerializedName("ammount_summoned")
            private @NotNull ConcurrentMap<Type, Integer> amountSummoned = Concurrent.newMap();
            @SerializedName("fastest_kill")
            private @NotNull ConcurrentMap<Type, Integer> fastestKill = Concurrent.newMap();
            @SerializedName("most_damage")
            private @NotNull ConcurrentMap<Type, Double> mostDamage = Concurrent.newMap();
            @SerializedName("summoning_eyes_contributed")
            private @NotNull ConcurrentMap<Type, Integer> summoningEyesContributed = Concurrent.newMap();
            @SerializedName("ender_crystals_collected")
            private int enderCrystalsCollected;
            public enum Type {

                UNKNOWN,
                BEST,
                TOTAL,

                OLD,
                PROTECTOR,
                STRONG,
                SUPERIOR,
                UNSTABLE,
                WISE,
                YOUNG;

                public static @NotNull Type of(@NotNull String name) {
                    return Arrays.stream(values())
                        .filter(type -> type.name().equalsIgnoreCase(name))
                        .findFirst()
                        .orElse(UNKNOWN);
                }

            }

        }

    }

    @Getter
    public static class Winter {

        @SerializedName("most_damage_dealt")
        private int mostDamageDealt;
        @SerializedName("most_snowballs_hit")
        private int mostSnowballsHit;
        @SerializedName("most_magma_damage_dealt")
        private int mostMagmaDamageDealt;
        @SerializedName("most_cannonballs_hit")
        private int mostCannonballsHit;

    }

    @Getter
    public static class ItemsFished {

        @SerializedName("trophy_fish")
        private int trophyFish;
        private int normal;
        private int treasure;
        @SerializedName("large_treasure")
        private int largeTreasure;
        private int total;

    }

    @Getter
    public static class Gifts {

        @SerializedName("total_received")
        private int received;
        @SerializedName("total_given")
        private int given;

    }

    @Getter
    public static class PetStats {

        @SerializedPath("milestone.sea_creatures_killed")
        private int seaCreaturesKilled;
        @SerializedPath("milestone.ores_mined")
        private int oresMined;
        @SerializedName("total_exp_gained")
        private double totalExperienceGained;

    }

    @Getter
    public static class ShredderRod {

        private int fished;
        private int bait;

    }

}
