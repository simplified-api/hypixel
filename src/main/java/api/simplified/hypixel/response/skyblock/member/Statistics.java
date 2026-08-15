package api.simplified.hypixel.response.skyblock.member;

import api.simplified.skyblock.common.Rarity;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.EnumLookup;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Lenient;
import dev.simplified.gson.annotation.SerializedPath;
import org.jetbrains.annotations.NotNull;


/**
 * The counters Hypixel keeps for a member, gathered from every corner of the game.
 * <p>
 * This node is a grab bag rather than a feature, so the groupings below follow the wire rather
 * than anything in the game. Several of the maps are keyed by an enum, and a wire key that names
 * no constant binds under a null key rather than being dropped - the aggregate {@code total}
 * entry the auction counters carry is one such key.
 */
@Getter
public class Statistics {

    /**
     * Best times posted on each of the races.
     */
    private @NotNull Races races = new Races();

    /**
     * Mythological event counters from the Diana burrow hunt.
     */
    private @NotNull Mythos mythos = new Mythos();

    /**
     * Counters for the member's auction house activity.
     */
    private @NotNull Auctions auctions = new Auctions();

    /**
     * Candy collected across the spooky festivals.
     */
    @SerializedName("candy_collected")
    private @NotNull CandyCollected candy = new CandyCollected();

    /**
     * Counters tracked against pets rather than against the member.
     */
    private @NotNull PetStats pets = new PetStats();

    /**
     * Counters from the End island, including the dragon fights.
     */
    @SerializedName("end_island")
    private @NotNull EndIsland endIsland = new EndIsland();

    /**
     * Best results posted during the winter event on Jerry's island.
     */
    private @NotNull Winter winter = new Winter();

    /**
     * Sea creatures the member has killed while fishing.
     */
    @SerializedName("sea_creature_kills")
    private int seaCreatureKills;

    /**
     * What the member has pulled out of the water, split by catch.
     */
    @SerializedName("items_fished")
    private @NotNull ItemsFished itemsFished = new ItemsFished();

    /**
     * Gifts exchanged with other players during the winter event.
     */
    private @NotNull Gifts gifts = new Gifts();

    /**
     * Counters particular to the Shredder fishing rod.
     */
    @SerializedName("shredder_rod")
    private @NotNull ShredderRod shredderRod = new ShredderRod();

    /**
     * Times the member has killed each mob, keyed by the mob id.
     */
    private @NotNull ConcurrentMap<String, Integer> kills = Concurrent.newMap();

    /**
     * Times the member has been killed by each mob, keyed by the mob id.
     */
    private @NotNull ConcurrentMap<String, Integer> deaths = Concurrent.newMap();

    /**
     * Every Rift counter the wire carries, keyed as the wire names them.
     * <p>
     * The Rift sends these as a flat spread of counters rather than as a structured node, so they
     * are taken as they come. An entry whose value is not a number falls into overflow instead.
     */
    @Lenient
    @SerializedName("rift")
    private @NotNull ConcurrentMap<String, Integer> riftStats = Concurrent.newMap();

    /**
     * Vermin vacuumed in the West Village, lifted out of the Rift counters because the wire nests it
     * among them as an object rather than a number.
     */
    @Extract("riftStats.west_vermin_vacuumed")
    private @NotNull VerminVacuumed verminVacuumed = new VerminVacuumed();

    /**
     * Spooky bats spawned, keyed by the year of the festival.
     */
    @Lenient
    @SerializedPath("spooky.bats_spawned")
    private @NotNull ConcurrentMap<Integer, Integer> spawnedSpookyBats = Concurrent.newMap();

    /**
     * Glowing mushrooms the member has broken.
     */
    @SerializedName("glowing_mushrooms_broken")
    private int glowingMushroomsBroken;

    // Attribute Shards

    /**
     * Distinct attribute shards the member has absorbed.
     */
    @SerializedName("unique_shards")
    private int uniqueShards;

    /**
     * Combat shard hunts completed.
     */
    @SerializedName("shard_combat_hunts")
    private int combatShardHunts;

    /**
     * Fishing shard hunts completed.
     */
    @SerializedName("shard_fishing_hunts")
    private int fishingShardHunts;

    /**
     * Forest shard hunts completed.
     */
    @SerializedName("shard_forest_hunts")
    private int forestShardHunts;

    /**
     * Trap shard hunts completed.
     */
    @SerializedName("shard_trap_hunts")
    private int trapShardHunts;

    /**
     * Salt shard hunts completed.
     */
    @SerializedName("shard_salt_hunts")
    private int saltShardHunts;

    // Damage

    /**
     * The largest single hit the member has landed.
     */
    @SerializedName("highest_damage")
    private double highestDamage;

    /**
     * The largest critical hit the member has landed.
     */
    @SerializedName("highest_critical_damage")
    private double highestCriticalDamage;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)

    /**
     * Best times the member has posted on each race.
     */
    public static class Races {

        /**
         * Best time on each dungeon hub race course, keyed by the course the wire names.
         */
        @SerializedName("dungeon_hub")
        private @NotNull ConcurrentMap<String, Integer> dungeonHub = Concurrent.newMap();

        /**
         * Best time on the chicken race.
         */
        @SerializedName("chicken_race_best_time_2")
        private int chickenRaceBestTime;

        /**
         * Best time on the foraging race.
         */
        @SerializedName("foraging_race_best_time")
        private int foragingRaceBestTime;

        /**
         * Best time on the Rift race.
         */
        @SerializedName("rift_race_best_time")
        private int riftRaceBestTime;

        /**
         * Best time on the End race.
         */
        @SerializedName("end_race_best_time")
        private int endRaceBestTime;

    }

    @Getter

    /**
     * Counters from the mythological burrow hunt run with a griffin pet.
     */
    public static class Mythos {

        /**
         * Mythological creatures the member has killed.
         */
        private int kills;

        /**
         * Burrow chains carried through to their end, keyed by the rarity the chain started at.
         */
        @SerializedName("burrows_chains_complete")
        private @NotNull ConcurrentMap<Type, Integer> completedChains = Concurrent.newMap();

        /**
         * Burrows dug that held treasure, keyed by rarity.
         */
        @SerializedName("burrows_dug_treasure")
        private @NotNull ConcurrentMap<Type, Integer> dugTreasure = Concurrent.newMap();

        /**
         * Burrows dug that spawned a creature, keyed by rarity.
         */
        @SerializedName("burrows_dug_combat")
        private @NotNull ConcurrentMap<Type, Integer> dugCombat = Concurrent.newMap();

        /**
         * Burrows dug that pointed at another burrow, keyed by rarity.
         */
        @SerializedName("burrows_dug_next")
        private @NotNull ConcurrentMap<Type, Integer> dugNext = Concurrent.newMap();

        /**
         * The rarity a burrow was found at, plus the aggregate row the wire mixes in.
         */
        public enum Type {

            @SerializedName("null")

            /**
             * A burrow with no rarity recorded.
             * <p>
             * The wire spells this {@code none}, which binds because the lookup also accepts the
             * constant name, but it is written back out as {@code null} - so this member does not
             * round-trip.
             */
            NONE,

            /**
             * A burrow found at common rarity.
             */
            COMMON,

            /**
             * A burrow found at epic rarity.
             */
            EPIC,

            /**
             * A burrow found at legendary rarity.
             */
            LEGENDARY,

            /**
             * A burrow found at mythic rarity.
             */
            MYTHIC,

            /**
             * The aggregate row the wire carries alongside the rarities.
             */
            TOTAL

        }

    }

    @Getter

    /**
     * Counters for the member's buying and selling on the auction house.
     * <p>
     * The two totals are keyed by item rarity, and the wire adds an aggregate {@code total} entry
     * that names no rarity - each map takes it into overflow rather than binding it, so what a
     * caller walks is rarities and nothing else.
     */
    public static class Auctions {

        // Total

        /**
         * Items bought, keyed by the item rarity.
         */
        @Lenient
        @SerializedName("total_bought")
        private @NotNull ConcurrentMap<Rarity, Integer> totalBought = Concurrent.newMap();

        /**
         * Items sold, keyed by the item rarity.
         */
        @Lenient
        @SerializedName("total_sold")
        private @NotNull ConcurrentMap<Rarity, Integer> totalSold = Concurrent.newMap();

        /**
         * Auctions the member has won.
         */
        @SerializedName("won")
        private int totalWon;

        /**
         * Auctions the member has created.
         */
        @SerializedName("created")
        private int totalCreated;

        /**
         * Auctions the member has seen through to a sale.
         */
        @SerializedName("completed")
        private int totalCompleted;

        // Gold

        /**
         * Coins spent buying.
         */
        @SerializedName("gold_spent")
        private long goldSpent;

        /**
         * Coins earned selling.
         */
        @SerializedName("gold_earned")
        private long goldEarned;

        /**
         * Coins paid in listing fees.
         */
        @SerializedName("fees")
        private long goldFees;

        // Bids

        /**
         * Bids the member has placed.
         */
        private int bids;

        /**
         * Own auctions that ended with no bid at all.
         */
        @SerializedName("no_bids")
        private int noBids;

        /**
         * The largest bid the member has placed.
         */
        @SerializedName("highest_bid")
        private long highestBid;

    }

    @Getter

    /**
     * Counters from the End island.
     */
    public static class EndIsland {

        /**
         * Counters from the ender dragon fights.
         */
        @SerializedName("dragon_fight")
        private DragonFight dragonFight = new DragonFight();

        /**
         * Summoning eyes the member has collected.
         */
        @SerializedName("summoning_eyes_collected")
        private int summoningEyesCollected;

        /**
         * Special zealot drops the member has collected.
         */
        @SerializedName("special_zealot_loot_collected")
        private int specialZealotLootCollected;

        @Getter

        /**
         * Counters kept per kind of ender dragon.
         */
        public static class DragonFight {

            /**
             * Best placement the member has taken in a fight, keyed by dragon.
             */
            @SerializedName("highest_rank")
            private @NotNull ConcurrentMap<Type, Integer> highestRank = Concurrent.newMap();

            /**
             * Times each dragon has been summoned while the member was present.
             */
            @SerializedName("amount_summoned")
            private @NotNull ConcurrentMap<Type, Integer> amountSummoned = Concurrent.newMap();

            /**
             * Fastest kill recorded, keyed by dragon.
             */
            @SerializedName("fastest_kill")
            private @NotNull ConcurrentMap<Type, Integer> fastestKill = Concurrent.newMap();

            /**
             * Most damage the member has dealt in one fight, keyed by dragon.
             */
            @SerializedName("most_damage")
            private @NotNull ConcurrentMap<Type, Double> mostDamage = Concurrent.newMap();

            /**
             * Summoning eyes the member has placed, keyed by dragon.
             */
            @SerializedName("summoning_eyes_contributed")
            private @NotNull ConcurrentMap<Type, Integer> summoningEyesContributed = Concurrent.newMap();

            /**
             * Ender crystals the member has destroyed.
             */
            @SerializedName("ender_crystals_destroyed")
            private int enderCrystalsDestroyed;

            /**
             * The kind of ender dragon a counter belongs to, plus the aggregate rows the wire
             * mixes in.
             */
            @EnumLookup
            public enum Type {

                /**
                 * A dragon the wire named that this enum does not carry.
                 */
                UNKNOWN,

                /**
                 * The aggregate best row across every dragon.
                 */
                BEST,

                /**
                 * The aggregate total row across every dragon.
                 */
                TOTAL,

                /**
                 * The Old Dragon.
                 */
                OLD,

                /**
                 * The Protector Dragon.
                 */
                PROTECTOR,

                /**
                 * The Strong Dragon.
                 */
                STRONG,

                /**
                 * The Superior Dragon.
                 */
                SUPERIOR,

                /**
                 * The Unstable Dragon.
                 */
                UNSTABLE,

                /**
                 * The Wise Dragon.
                 */
                WISE,

                /**
                 * The Young Dragon.
                 */
                YOUNG

            }

        }

    }

    @Getter

    /**
     * Best results the member posted during the winter event.
     */
    public static class Winter {

        /**
         * Most damage dealt in one winter event run.
         */
        @SerializedName("most_damage_dealt")
        private int mostDamageDealt;

        /**
         * Most snowballs landed in one run.
         */
        @SerializedName("most_snowballs_hit")
        private int mostSnowballsHit;

        /**
         * Most magma damage dealt in one run.
         */
        @SerializedName("most_magma_damage_dealt")
        private int mostMagmaDamageDealt;

        /**
         * Most cannonballs landed in one run.
         */
        @SerializedName("most_cannonballs_hit")
        private int mostCannonballsHit;

    }

    @Getter

    /**
     * What the member has pulled out of the water, split by catch.
     */
    public static class ItemsFished {

        /**
         * Trophy fish caught.
         */
        @SerializedName("trophy_fish")
        private int trophyFish;

        /**
         * Trophy frogs caught.
         */
        @SerializedName("trophy_frog")
        private int trophyFrog;

        /**
         * Ordinary catches with nothing special about them.
         */
        private int normal;

        /**
         * Treasure chests fished up.
         */
        private int treasure;

        /**
         * Large treasure chests fished up.
         */
        @SerializedName("large_treasure")
        private int largeTreasure;

        /**
         * Outstanding catches, the rarest of the treasure tiers.
         */
        private int outstanding;

        /**
         * Vermin of every kind added together.
         */
        private int total;

    }

    @Getter

    /**
     * Gifts exchanged with other players during the winter event.
     */
    public static class Gifts {

        /**
         * Gifts the member has been given.
         */
        @SerializedName("total_received")
        private int received;

        /**
         * Gifts the member has handed out.
         */
        @SerializedName("total_given")
        private int given;

    }

    @Getter

    /**
     * Counters tracked against the pets a member has had out.
     */
    public static class PetStats {

        /**
         * Sea creatures killed toward the pet milestone.
         */
        @SerializedPath("milestone.sea_creatures_killed")
        private int seaCreaturesKilled;

        /**
         * Ores mined toward the pet milestone.
         */
        @SerializedPath("milestone.ores_mined")
        private int oresMined;

        /**
         * Experience the member has fed to pets in total.
         */
        @SerializedName("total_exp_gained")
        private double totalExperienceGained;

    }

    @Getter

    /**
     * Counters particular to the Shredder fishing rod.
     */
    public static class ShredderRod {

        /**
         * Catches made with the Shredder.
         */
        private int fished;

        /**
         * Bait the Shredder has consumed.
         */
        private int bait;

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)

    /**
     * Candy collected across every spooky festival, and per festival.
     */
    public static class CandyCollected {

        /**
         * Every catch added together.
         */
        private int total;

        /**
         * Purple candy collected.
         */
        @SerializedName("purple_candy")
        private int purpleCandy;

        /**
         * Green candy collected.
         */
        @SerializedName("green_candy")
        private int greenCandy;

        /**
         * Candy collected at each festival, keyed by the year the wire names.
         */
        @Capture
        private @NotNull ConcurrentMap<String, FestivalCandy> festivals = Concurrent.newMap();

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)

        /**
         * One spooky festival's candy haul.
         */
        public static class FestivalCandy {

            /**
             * Candy of every colour added together for this festival.
             */
            private int total;

            /**
             * Purple candy collected at this festival.
             */
            @SerializedName("purple_candy")
            private int purpleCandy;

            /**
             * Green candy collected at this festival.
             */
            @SerializedName("green_candy")
            private int greenCandy;

        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)

    /**
     * Vermin vacuumed up in the Rift, split by creature.
     */
    public static class VerminVacuumed {

        /**
         * Candy of every colour added together.
         */
        private int total;

        /**
         * Silverfish vacuumed.
         */
        private int silverfish;

        /**
         * Spiders vacuumed.
         */
        private int spider;

        /**
         * Mosquitoes vacuumed.
         */
        private int mosquito;

    }

}
