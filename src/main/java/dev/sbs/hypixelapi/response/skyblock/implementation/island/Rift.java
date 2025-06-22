package dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island;

import com.google.gson.annotations.SerializedName;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.pet.Pet;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.implementation.island.util.NbtContent;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.SerializedPath;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;

@Getter
public class Rift {

    // Boundaries
    @SerializedName("village_plaza")
    private VillagePlaza villagePlaza = new VillagePlaza();
    @SerializedName("wither_cage")
    private WitherCage witherCage = new WitherCage();
    @SerializedName("black_lagoon")
    private BlackLagoon blackLagoon = new BlackLagoon();
    @SerializedName("dead_cats")
    private DeadCats deadCats = new DeadCats();
    @SerializedName("wizard_tower")
    private WizardTower wizardTower = new WizardTower();
    @SerializedName("west_village")
    private WestVillage westVillage = new WestVillage();
    private Enigma enigma = new Enigma();
    @SerializedName("wyld_woods")
    private WyldWoods wyldWoods = new WyldWoods();
    private Gallery gallery = new Gallery();
    private Castle castle = new Castle();
    private Access access = new Access();
    @SerializedName("slayer_quest")
    private SlayerQuest slayerQuest = new SlayerQuest();
    private Dreadfarm dreadfarm = new Dreadfarm();

    @SerializedName("lifetime_purchased_boundaries")
    private @NotNull ConcurrentList<String> lifetimePurchasedBoundaries = Concurrent.newList();

    // Inventories
    @SerializedPath("inventory.inv_contents")
    private NbtContent inventory = new NbtContent();
    @SerializedPath("inventory.inv_armor")
    private NbtContent armor = new NbtContent();
    @SerializedPath("inventory.ender_chest_contents")
    private NbtContent enderChest = new NbtContent();
    @SerializedPath("inventory.equipment_contents")
    private NbtContent equipment = new NbtContent();


    @Getter
    public static class VillagePlaza {

        @Accessors(fluent = true)
        @SerializedName("got_scammed")
        private boolean hasGotScammed;
        private Murder murder = new Murder();
        @SerializedName("barry_center")
        private BarryCenter barryCenter = new BarryCenter();
        private Cowboy cowboy = new Cowboy();
        private Lonely lonely = new Lonely();
        private Seraphine seraphine = new Seraphine();

        @Getter
        public static class Murder {

            @SerializedName("step_index")
            private int stepIndex;
            @SerializedName("room_clues")
            private @NotNull ConcurrentList<String> roomClues = Concurrent.newList();

        }

        @Getter
        public static class BarryCenter {

            @SerializedName("first_talk_to_barry")
            private boolean firstTalkToBarry;
            @SerializedName("received_reward")
            private boolean receivedReward;
            private @NotNull ConcurrentList<String> convinced = Concurrent.newList();

        }

        @Getter
        public static class Cowboy {

            private int stage;
            @SerializedName("hay_eaten")
            private int hayEaten;
            @SerializedName("rabbit_name")
            private String rabbitName;
            @SerializedName("exported_carrots")
            private int exportedCarrots;

        }

        @Getter
        public static class Lonely {

            @SerializedName("seconds_sitting")
            private int secondsSitting;

        }

        @Getter
        public static class Seraphine {

            @SerializedName("step_index")
            private int stepIndex;

        }

    }

    @Getter
    public static class WitherCage {

        @SerializedName("killed_eyes")
        private @NotNull ConcurrentList<String> killedEyes = Concurrent.newList();

    }

    @Getter
    public static class BlackLagoon {

        @Accessors(fluent = true)
        @SerializedName("talked_to_edwin")
        private boolean hasTalkedToEdwin;
        @Accessors(fluent = true)
        @SerializedName("received_science_paper")
        private boolean hasReceivedSciencePaper;
        @Accessors(fluent = true)
        @SerializedName("delivered_science_paper")
        private boolean hasDeliveredSciencePaper;
        @SerializedName("completed_step")
        private int completedStep;

    }

    @Getter
    public static class DeadCats {

        @Accessors(fluent = true)
        @SerializedName("talked_to_jacquelle")
        private boolean hasTalkedToJacquelle;
        @Accessors(fluent = true)
        @SerializedName("picked_up_detector")
        private boolean hasPickedUpDetector;
        @SerializedName("found_cats")
        private @NotNull ConcurrentList<String> foundCats = Concurrent.newList();
        @Accessors(fluent = true)
        @SerializedName("unlocked_pet")
        private boolean hasUnlockedPet;
        private Optional<Pet> montezuma = Optional.empty();

    }

    @Getter
    public static class WizardTower {

        @SerializedName("wizard_quest_step")
        private int wizardQuestStep;
        @SerializedName("crumbs_laid_out")
        private int crumbsLaidOut;

    }

    @Getter
    public static class WestVillage {

        @SerializedName("crazy_kloon")
        private CrazyKloon crazyKloon = new CrazyKloon();
        private Mirrorverse mirrorverse = new Mirrorverse();
        @SerializedName("kat_house")
        private KatHouse katHouse = new KatHouse();
        private Glyphs glyphs = new Glyphs();

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class CrazyKloon {

            @SerializedName("selected_colors")
            private @NotNull ConcurrentMap<String, String> selectedColors = Concurrent.newMap();
            @Accessors(fluent = true)
            @SerializedName("talked")
            private boolean hasTalked;
            @SerializedName("hacked_terminals")
            private @NotNull ConcurrentList<String> hackedTerminals = Concurrent.newList();
            @SerializedName("quest_complete")
            private boolean questComplete;

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Mirrorverse {

            @SerializedName("visited_rooms")
            private @NotNull ConcurrentList<String> visitedRooms = Concurrent.newList();
            @SerializedName("upside_down_hard")
            private boolean upsideDownHardCompleted;
            @SerializedName("claimed_chest_items")
            private @NotNull ConcurrentList<String> claimedChestItems = Concurrent.newList();
            @SerializedName("claimed_reward")
            private boolean claimedReward;

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class KatHouse {

            @SerializedName("bin_collected_mosquito")
            private int collectedMosquito;
            @SerializedName("bin_collected_spider")
            private int collectedSpider;
            @SerializedName("bin_collected_silverfish")
            private int collectedSilverfish;

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Glyphs {

            @SerializedName("claimed_wand")
            private boolean claimedWand;
            @SerializedName("current_glyph_delivered")
            private boolean currentGlyphDelivered;
            @SerializedName("current_glyph_completed")
            private boolean currentGlyphCompleted;
            @SerializedName("current_glyph")
            private int currentGlyph;
            private boolean completed;
            @SerializedName("claimed_bracelet")
            private boolean claimedBracelet;

        }

    }

    @Getter
    public static class Enigma {

        @Accessors(fluent = true)
        @SerializedName("bought_cloak")
        private boolean hasBoughtCloak;
        @SerializedName("found_souls")
        private @NotNull ConcurrentList<String> foundSouls = Concurrent.newList();
        @SerializedName("claimed_bonus_index")
        private int claimedBonusIndex;

    }

    @Getter
    public static class WyldWoods {

        @SerializedName("talked_threebrothers")
        private @NotNull ConcurrentList<String> talkedThreebrothers = Concurrent.newList();
        @SerializedName("bughunter_step")
        private int bughunterStep;
        @Accessors(fluent = true)
        @SerializedName("sirius_started_q_a")
        private boolean hasStartedSiriusQA;
        @SerializedName("sirius_q_a_chain_done")
        private boolean siriusQAChainDone;
        @Accessors(fluent = true)
        @SerializedName("sirius_completed_q_a")
        private boolean hasCompletedSiriusQA;
        @Accessors(fluent = true)
        @SerializedName("sirius_claimed_doubloon")
        private boolean hasClaimedSiriusDoubloon;

    }

    @Getter
    public static class Gallery {

        @SerializedName("elise_step")
        private int eliseStep;
        @SerializedName("secured_trophies")
        private @NotNull ConcurrentList<Trophy> securedTrophies = Concurrent.newList();

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Trophy {

            private String type;
            private Instant timestamp;
            private int visits;

        }

    }

    @Getter
    public static class Castle {

        @Accessors(fluent = true)
        @SerializedName("unlocked_pathway_skip")
        private boolean hasUnlockedPathwaySkip;
        @SerializedName("fairy_step")
        private int fairyStep;
        @SerializedName("grubber_stacks")
        private int grubberStacks;

    }

    @Getter
    public static class Access {

        @SerializedName("last_free")
        private long lastFree;
        @Accessors(fluent = true)
        @SerializedName("consumed_prism")
        private boolean hasConsumedPrism;

    }

    @Getter
    public static class SlayerQuest extends Slayer.Quest {

        @SerializedName("combat_xp")
        private int combatXP;
        @SerializedName("recent_mob_kills")
        private @NotNull ConcurrentList<MobKill> recentMobKills = Concurrent.newList();
        @SerializedName("last_killed_mob_island")
        private String lastKilledMobIsland;

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class MobKill {

            private int xp;
            private Instant timestamp;

        }

    }

    @Getter
    public static class Dreadfarm {

        @SerializedName("shania_stage")
        private int shaniaStage;
        @SerializedName("caducous_feeder_uses")
        private @NotNull ConcurrentList<Instant> caducousFeederUses = Concurrent.newList();

    }

}
