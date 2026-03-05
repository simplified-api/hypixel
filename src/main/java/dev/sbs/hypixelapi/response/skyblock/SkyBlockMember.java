package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.SimplifiedApi;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.collection.concurrent.ConcurrentSet;
import dev.sbs.api.collection.concurrent.linked.ConcurrentLinkedMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.api.tuple.pair.PairOptional;
import dev.sbs.api.util.NumberUtil;
import dev.sbs.api.util.StringUtil;
import dev.sbs.api.util.mutable.MutableDouble;
import dev.sbs.minecraftapi.MinecraftApi;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.BestiaryData;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.CrimsonIsle;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.PetData;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.PlayerData;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.Rift;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.SkillData;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.SlayerData;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.StatData;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.dungeon.DungeonProfile;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining.ForgeItem;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining.GlaciteTunnels;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining.Mining;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.profile_stats.data.AccessoryData;
import dev.sbs.minecraftapi.nbt.tags.collection.CompoundTag;
import dev.sbs.minecraftapi.nbt.tags.primitive.StringTag;
import dev.sbs.minecraftapi.skyblock.common.NbtContent;
import dev.sbs.minecraftapi.skyblock.common.Weight;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import dev.sbs.minecraftapi.skyblock.model.Accessory;
import dev.sbs.minecraftapi.skyblock.model.Power;
import dev.sbs.minecraftapi.skyblock.model.Stat;
import dev.sbs.minecraftapi.skyblock.model.TrophyFish;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Getter
@NoArgsConstructor
public class SkyBlockMember implements PostInit {
    
    // Profile
    @SerializedName("player_id")
    private @NotNull UUID uniqueId;
    @SerializedName("first_join_hub")
    private SkyBlockDate.SkyBlockTime firstJoinHub;
    @SerializedPath("profile.first_join")
    private SkyBlockDate.RealTime firstJoin;
    @SerializedPath("profile.personal_bank_upgrade")
    private int personalBankUpgrade;
    @SerializedPath("profile.cookie_buff_active")
    private boolean boosterCookieActive;

    // Data
    @SerializedName("player_data")
    private @NotNull PlayerData playerData = new PlayerData();
    @SerializedName("slayer")
    private @NotNull SlayerData slayerData = new SlayerData();
    @SerializedName("pets_data")
    private @NotNull PetData petData = new PetData();
    @SerializedName("dungeons")
    private @NotNull DungeonProfile dungeonData = new DungeonProfile();
    @SerializedName("player_stats")
    private @NotNull StatData statData = new StatData();
    private transient SkillData skillData;

    // Locations
    private @NotNull Rift rift = new Rift();

    // Mining
    private @NotNull Mining mining = new Mining();
    @SerializedPath("forge.forge_processes.forge_1")
    private @NotNull ConcurrentMap<Integer, ForgeItem> forge = Concurrent.newMap();
    @SerializedName("glacite_player_data")
    private @NotNull GlaciteTunnels glaciteTunnels = new GlaciteTunnels();

    private @NotNull BestiaryData bestiary = new BestiaryData();
    @SerializedName("accessory_bag_storage")
    private @NotNull AccessoryBag accessoryBag = new AccessoryBag();
    private @NotNull Leveling leveling = new Leveling();
    @SerializedName("nether_island_player_data")
    private @NotNull CrimsonIsle crimsonIsle = new CrimsonIsle();
    private @NotNull Experimentation experimentation = new Experimentation();
    @SerializedName("fairy_soul")
    private @NotNull FairySouls fairySouls = new FairySouls();
    private @NotNull Currencies currencies = new Currencies();
    @SerializedName("item_data")
    private @NotNull ItemSettings itemSettings = new ItemSettings();
    @SerializedName("jacobs_contest")
    private @NotNull JacobsContest jacobsContest = new JacobsContest();
    private @NotNull Inventory inventory = new Inventory();
    private @NotNull Optional<Quests> quests = Optional.empty();

    // Maps
    @SerializedName("trophy_fish")
    @Getter(AccessLevel.NONE)
    private @NotNull ConcurrentMap<String, Object> trophyFishMap = Concurrent.newMap();
    private transient TrophyFishing trophyFish;
    private @NotNull ConcurrentMap<String, Long> collection = Concurrent.newMap();
    private transient @NotNull ConcurrentMap<String, Integer> collectionUnlocked = Concurrent.newMap();
    @SerializedPath("objectives.tutorial")
    private @NotNull ConcurrentList<String> tutorialObjectives = Concurrent.newList();

    @Override
    public void postInit() {
        this.accessoryBag.initialize(this);
        this.trophyFish = new TrophyFishing(this.trophyFishMap);
        this.skillData = new SkillData(this.getPlayerData().getSkillExperience(), this);

        this.collectionUnlocked = this.getCollection()
            .stream()
            .map((itemId, value) -> Pair.of(itemId, this.getPlayerData()
                .getUnlockedCollectionTiers()
                .stream()
                .filter(tier -> tier.matches(String.format("^%s_[\\d]+$", itemId)))
                .map(tier -> Integer.parseInt(tier.replace(String.format("%s_", itemId), "")))
                .max(Comparator.naturalOrder())
                .orElse(0)
            ))
            .collect(Concurrent.toUnmodifiableMap());
    }

    public @NotNull ConcurrentList<Integer> getCraftedMinions(@NotNull String itemId) {
        return this.getPlayerData().getCraftedMinions(itemId);
    }

    // Weight

    public @NotNull Weight getTotalWeight() {
        // Load Weights
        Weight skillWeight = this.getTotalWeight(member -> member.getSkillData().getWeight());
        Weight slayerWeight = this.getTotalWeight(member -> member.getSlayerData().getWeight());
        Weight dungeonWeight = this.getTotalWeight(member -> member.getDungeonData().getWeight());
        Weight dungeonClassWeight = this.getTotalWeight(member -> member.getDungeonData().getClassWeight());

        return Weight.of(
            skillWeight.getValue() + slayerWeight.getValue() + dungeonWeight.getValue() + dungeonClassWeight.getValue(),
            skillWeight.getOverflow() + slayerWeight.getOverflow() + dungeonWeight.getOverflow() + dungeonClassWeight.getOverflow()
        );
    }

    private @NotNull Weight getTotalWeight(@NotNull Function<SkyBlockMember, ConcurrentMap<?, Weight>> weightMapFunction) {
        MutableDouble totalWeight = new MutableDouble();
        MutableDouble totalOverflow = new MutableDouble();

        weightMapFunction.apply(this)
            .stream()
            .map(Map.Entry::getValue)
            .forEach(skillWeight -> {
                totalWeight.add(skillWeight.getValue());
                totalOverflow.add(skillWeight.getOverflow());
            });

        return Weight.of(totalWeight.get(), totalOverflow.get());
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AccessoryBag {

        @SerializedName("bag_upgrades_purchased")
        private int bagUpgradesPurchased;
        private transient NbtContent contents = new NbtContent();
        private transient @NotNull ConcurrentList<AccessoryData> detectedAccessories = Concurrent.newUnmodifiableList();
        private transient @NotNull ConcurrentList<AccessoryData> accessories = Concurrent.newUnmodifiableList();

        // Power
        @SerializedName("selected_power")
        private @NotNull Optional<String> selectedPowerId = Optional.empty();
        @SerializedName("unlocked_powers")
        private @NotNull ConcurrentList<String> unlockedPowerIds = Concurrent.newUnmodifiableList();
        private transient @NotNull ConcurrentMap<String, Double> selectedPowerStats = Concurrent.newUnmodifiableMap();

        public @NotNull Optional<Power> getSelectedPower() {
            return this.getSelectedPowerId().flatMap(powerId -> MinecraftApi.getRepository(Power.class)
                .findFirst(Power::getId, powerId)
            );
        }

        public @NotNull ConcurrentList<Power> getUnlockedPowers() {
            return this.getUnlockedPowerIds()
                .stream()
                .map(powerId -> MinecraftApi.getRepository(Power.class)
                    .findFirst(Power::getId, powerId)
                )
                .flatMap(Optional::stream)
                .collect(Concurrent.toUnmodifiableList());
        }

        // Magical Power
        @SerializedName("highest_magical_power")
        private int highestMagicalPower;
        private transient int magicalPower;
        private transient double logComponent;

        // Tuning
        private @NotNull Tuning tuning = new Tuning();
        private transient int tuningPoints;

        protected void initialize(@NotNull SkyBlockMember member) {
            // Read Accessory Bag
            this.detectedAccessories = this.getContents()
                .getNbtData()
                .<CompoundTag>getListTag("i")
                .stream()
                .filter(CompoundTag::notEmpty)
                .flatMap(compoundTag -> SimplifiedApi.getRepository(Accessory.class)
                    .findFirst(
                        Accessory::getId,
                        compoundTag.getPathOrDefault("tag.ExtraAttributes.id", StringTag.EMPTY).getValue()
                    )
                    .map(accessory -> Pair.of(accessory, compoundTag))
                    .stream()
                )
                .map(entry -> new AccessoryData(entry.getKey(), entry.getValue()))
                .collect(Concurrent.toList());

            // Store Families
            ConcurrentMap<String, ConcurrentSet<Accessory>> familyAccessoryDataMap = Concurrent.newMap();
            this.getDetectedAccessories()
                .stream()
                .filter(accessoryData -> accessoryData.getAccessory().getFamily().isPresent())
                .forEach(accessoryData -> {
                    // New Accessory Family
                    String familyId = accessoryData.getAccessory().getFamily().get().getId();
                    if (!familyAccessoryDataMap.containsKey(familyId))
                        familyAccessoryDataMap.put(familyId, Concurrent.newSet());

                    // Store Accessory
                    familyAccessoryDataMap.get(familyId).add(accessoryData.getAccessory());
                });

            // Store Non-Stackable Families
            ConcurrentSet<Accessory> processedAccessories = Concurrent.newSet();
            this.accessories = this.getDetectedAccessories()
                .stream()
                .filter(accessoryData -> {
                    if (accessoryData.getAccessory().getFamily().isPresent()) {
                        // Handle Families
                        ConcurrentList<Accessory> familyData = Concurrent.newList(familyAccessoryDataMap.get(
                            accessoryData.getAccessory().getFamily().get().getId()
                        ));

                        if (accessoryData.getAccessory().getFamily().get().getRank() >= 0) {
                            // Sort By Highest
                            familyData = familyData.sorted(accessory -> accessory.getFamily()
                                    .map(Accessory.Family::getRank)
                                    .orElse(0)
                                )
                                .inverse();

                            // Ignore Lowest Accessories
                            Accessory topAccessory = familyData.remove(0);
                            processedAccessories.addAll(familyData);

                            // Top Accessory Only
                            if (!accessoryData.getAccessory().equals(topAccessory))
                                return false;
                        } else {
                            if (processedAccessories.contains(accessoryData.getAccessory()))
                                return false;

                            // Ignore All Accessories
                            processedAccessories.addAll(familyData);
                            return true;
                        }
                    }

                    return processedAccessories.add(accessoryData.getAccessory());
                })
                .collect(Concurrent.toList());

            // Calculate Magical Power
            int calculatedMagicalPower = this.getAccessories()
                .stream()
                .mapToInt(accessoryData -> this.handleMagicalPower(accessoryData, member))
                .sum();

            // Rift Prism
            if (member.getRift().getAccess().hasConsumedPrism())
                calculatedMagicalPower += 11;

            this.contents = member.getInventory().getBags().getAccessories();
            this.magicalPower = calculatedMagicalPower;
            this.tuningPoints = this.magicalPower / 10;
            this.logComponent = Math.pow(Math.log(1 + (0.0019 * this.magicalPower)), 1.2);
            //this.magicalPowerMultiplier = 29.97 * Math.pow(Math.log(1 + (0.0019 * this.magicalPower)), 1.2);

            // Power Stats
            ConcurrentMap<String, Double> stats = this.getSelectedPower()
                .stream()
                .flatMap(power -> power.getBaseValues().stream())
                .map(entry -> Pair.of(
                    entry.getKey(),
                    MinecraftApi.getRepository(Stat.class)
                        .findFirstOrNull(Stat::getId, entry.getKey())
                        .getPowerCoefficient() * this.getLogComponent() * entry.getValue()
                ))
                .collect(Concurrent.toUnmodifiableMap());

            this.getSelectedPower().ifPresent(power -> power.getBonuses()
                .forEach((statId, value) -> stats.merge(
                    statId,
                    value,
                    Double::sum
                ))
            );

            this.selectedPowerStats = stats;
        }

        private int handleMagicalPower(AccessoryData accessoryData, SkyBlockMember member) {
            int magicalPower = accessoryData.getRarity().getMagicPower();

            // TODO: Dynamic
            if (accessoryData.getAccessory().getId().equals("HEGEMONY_ARTIFACT"))
                magicalPower *= 2;

            if (accessoryData.getAccessory().getId().equals("ABICASE"))
                magicalPower += member.getCrimsonIsle().getAbiphone().getContacts().size() / 2;

            return magicalPower;
        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Tuning implements PostInit {

            @SerializedName("highest_unlocked_slot")
            private int highestUnlockedSlot;
            @SerializedName("refund_1")
            @Accessors(fluent = true)
            private boolean hasClaimedRefund;

            // Slots
            @SerializedName("slot_0")
            private @NotNull ConcurrentMap<String, Integer> selected = Concurrent.newMap();
            @Getter(AccessLevel.NONE)
            private @NotNull ConcurrentMap<String, Integer> slot_1 = Concurrent.newMap();
            @Getter(AccessLevel.NONE)
            private @NotNull ConcurrentMap<String, Integer> slot_2 = Concurrent.newMap();
            @Getter(AccessLevel.NONE)
            private @NotNull ConcurrentMap<String, Integer> slot_3 = Concurrent.newMap();
            @Getter(AccessLevel.NONE)
            private @NotNull ConcurrentMap<String, Integer> slot_4 = Concurrent.newMap();

            // PostInit
            private @NotNull ConcurrentList<ConcurrentMap<String, Integer>> slots = Concurrent.newList();

            @Override
            public void postInit() {
                this.slots = Concurrent.newUnmodifiableList(
                    this.slot_1,
                    this.slot_2,
                    this.slot_3,
                    this.slot_4
                );
            }

        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class BagContents {

        @SerializedName("fishing_bag")
        private NbtContent fishing = new NbtContent();
        private NbtContent quiver = new NbtContent();
        @SerializedName("fishing_bag")
        private NbtContent accessories = new NbtContent();
        @SerializedName("potion_bag")
        private NbtContent potions = new NbtContent();

    }
    
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Currencies {

        @SerializedName("motes_purse")
        private int motes;
        @SerializedName("coin_purse")
        private double purse;
        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, ConcurrentMap<String, Integer>> essence = Concurrent.newMap();

        public @NotNull ConcurrentMap<String, Integer> getEssence() {
            return this.essence.stream()
                .mapValue(value -> value.get("current"))
                .collect(Concurrent.toMap());
        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Experimentation implements PostInit {

        @SerializedName("claims_resets")
        private int resetClaims;
        @SerializedName("claims_resets_timestamp")
        private Optional<SkyBlockDate.RealTime> resetClaimsTimestamp = Optional.empty();
        @SerializedName("serums_drank")
        private int serumsDrank;

        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, Long> pairings = Concurrent.newMap();
        private transient @NotNull Optional<Table> superpairs = Optional.empty();

        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, Long> simon = Concurrent.newMap();
        private transient @NotNull Optional<Table> chronomatron = Optional.empty();

        @Getter(AccessLevel.NONE)
        private @NotNull ConcurrentMap<String, Long> numbers = Concurrent.newMap();
        private transient @NotNull Optional<Table> ultrasequencer = Optional.empty();

        @Override
        public void postInit() {
            this.superpairs = Optional.of(new Table(this.pairings));
            this.chronomatron = Optional.of(new Table(this.simon));
            this.ultrasequencer = Optional.of(new Table(this.numbers));
        }

        @Getter
        public static class Table {

            private final @NotNull SkyBlockDate.RealTime lastAttempt;
            private final @NotNull SkyBlockDate.RealTime lastClaimed;
            private final int bonusClicks;
            private final @NotNull ConcurrentMap<Integer, Integer> attempts;
            private final @NotNull ConcurrentMap<Integer, Integer> claims;
            private final @NotNull ConcurrentMap<Integer, Integer> bestScore;

            private Table(@NotNull ConcurrentMap<String, Long> tableData) {
                this.lastAttempt = new SkyBlockDate.RealTime(tableData.removeOrGet("last_attempt", 0L));
                this.lastClaimed = new SkyBlockDate.RealTime(tableData.removeOrGet("last_claimed", 0L));
                this.bonusClicks = tableData.removeOrGet("bonus_clicks", 0L).intValue();

                ConcurrentMap<String, ConcurrentMap<Integer, Integer>> filteredData = Concurrent.newMap();

                tableData.forEach((key, value) -> {
                    if (!filteredData.containsKey(key))
                        filteredData.put(key, Concurrent.newMap());

                    String actual = key.substring(0, key.lastIndexOf("_"));
                    filteredData.get(key).put(Integer.parseInt(key.replace(String.format("%s_", actual), "")), value.intValue());
                });

                this.attempts = filteredData.removeOrGet("attempts", Concurrent.newMap());
                this.claims = filteredData.removeOrGet("claims", Concurrent.newMap());
                this.bestScore = filteredData.removeOrGet("best_score", Concurrent.newMap());
            }

        }

    }
    
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FairySouls {

        @SerializedName("total_collected")
        private int totalCollected;
        @SerializedName("fairy_exchanges")
        private int exchanges;
        @SerializedName("unspent_souls")
        private int unspent;

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Inventory {

        @SerializedName("inv_armor")
        private NbtContent armor = new NbtContent();
        @SerializedName("equipment_contents")
        private NbtContent equipment = new NbtContent();
        @SerializedName("wardrobe_contents")
        private NbtContent wardrobe = new NbtContent();
        @SerializedName("bag_contents")
        private BagContents bags = new BagContents();
        @SerializedName("inv_contents")
        private NbtContent content = new NbtContent();
        @SerializedName("wardrobe_equipped_slot")
        private int equippedWardrobeSlot;
        @SerializedName("backpack_icons")
        private ConcurrentMap<Integer, NbtContent> backpackIcons = Concurrent.newMap();
        @SerializedName("personal_vault_contents")
        private NbtContent personalVault = new NbtContent();
        @SerializedName("sacks_counts")
        private ConcurrentLinkedMap<String, Integer> sacks = Concurrent.newLinkedMap();
        @SerializedName("backpack_contents")
        private ConcurrentMap<Integer, NbtContent> backpacks = Concurrent.newMap();
        @SerializedName("ender_chest_contents")
        private NbtContent enderChest = new NbtContent();

    }
    
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ItemSettings {

        @SerializedName("teleporter_pill_consumed")
        private boolean teleporterPillConsumed;
        @SerializedName("favorite_arrow")
        private String favoriteArrow;
        private int soulflow;

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class JacobsContest implements PostInit {

        @SerializedName("medals_inv")
        private @NotNull ConcurrentMap<Medal, Integer> medals = Concurrent.newMap();
        @SerializedPath("perks.double_drops")
        private int doubleDrops;
        @SerializedPath("perks.farming_level_cap")
        private int farmingLevelCap;
        @Accessors(fluent = true)
        @SerializedName("talked")
        private boolean hasTalked;
        @Getter(AccessLevel.NONE)
        @SerializedName("contests")
        private @NotNull ConcurrentMap<String, Contest> contestMap = Concurrent.newMap();
        private @NotNull ConcurrentList<Contest> contests = Concurrent.newList();
        @SerializedName("unique_brackets")
        private @NotNull ConcurrentMap<Medal, ConcurrentList<String>> uniqueBrackets = Concurrent.newMap();
        private boolean migration;
        @SerializedName("personal_bests")
        private @NotNull ConcurrentMap<String, Integer> personalBests = Concurrent.newMap();

        @Override
        public void postInit() {
            this.contests = this.contestMap.stream()
                .map(entry -> {
                    Contest contest = entry.getValue();

                    String[] dataString = entry.getKey().split(":");
                    String[] calendarString = dataString[1].split("_");
                    int year = NumberUtil.toInt(dataString[0]);
                    int month = NumberUtil.toInt(calendarString[0]);
                    int day = NumberUtil.toInt(calendarString[1]);

                    contest.collectionName = StringUtil.join(dataString, ":", 2, dataString.length);
                    contest.skyBlockDate = new SkyBlockDate(year, month, day);
                    return contest;
                })
                .collect(Concurrent.toUnmodifiableList());
        }

        @Getter
        @RequiredArgsConstructor
        public enum Medal {

            DIAMOND(0.02, 0.05),
            PLATINUM(0.05, 0.1),
            GOLD(0.1, 0.2),
            SILVER(0.3, 0.4),
            BRONZE(0.6, 0.7),
            NONE(1.0, 1.0);

            private final double bracket;
            private final double finneganBracket;

            public static @NotNull Medal fromContest(@NotNull Contest contest) {
                return fromPosition(contest.getPosition(), contest.getParticipants(), contest.isFinnegan());
            }

            public static @NotNull Medal fromPosition(double position, double participants, boolean isFinnegan) {
                for (Medal medal : Medal.values()) {
                    double bracket = isFinnegan ? medal.getFinneganBracket() : medal.getBracket();

                    if (position <= Math.floor(participants * bracket))
                        return medal;
                }

                return NONE;
            }

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Contest {

            private int collected;
            @Accessors(fluent = true)
            @SerializedName("claimed_rewards")
            private boolean hasClaimedRewards;
            @SerializedName("claimed_position")
            private int position;
            @SerializedName("claimed_participants")
            private int participants;
            private SkyBlockDate skyBlockDate;
            private String collectionName;

            @Getter(AccessLevel.NONE)
            @SerializedName("claimed_medal")
            private @NotNull Optional<Medal> claimedMedal = Optional.empty();

            public @NotNull Medal getMedal() {
                return Medal.fromContest(this);
            }

            public boolean isFinnegan() {
                return this.claimedMedal.isPresent();
            }

        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Leveling {

        private int experience;
        private @NotNull ConcurrentMap<String, Integer> completions = Concurrent.newMap();
        private @NotNull ConcurrentList<String> completed = Concurrent.newList();
        @Accessors(fluent = true)
        @SerializedName("migrated_completions")
        private boolean hasMigratedCompletions;
        @SerializedName("category_expanded")
        private boolean categoryExpanded;
        @SerializedName("last_viewed_tasks")
        private @NotNull ConcurrentList<String> lastViewedTasks = Concurrent.newList();
        @SerializedName("completed_tasks")
        private @NotNull ConcurrentList<String> completedTasks = Concurrent.newList();
        @SerializedName("highest_pet_score")
        private int highestPetScore;
        @SerializedName("mining_fiesta_ores_mined")
        private int miningFiestaOresMined;
        private boolean migrated;
        @Accessors(fluent = true)
        @SerializedName("migrated_completions_2")
        private boolean hasMigratedCompletions2;
        @Accessors(fluent = true)
        @SerializedName("claimed_talisman")
        private boolean hasClaimedTalisman;
        @SerializedName("bop_bonus")
        private String bopBonus;
        @SerializedName("selected_symbol")
        private String selectedSymbol;
        @SerializedName("emblem_unlocks")
        private @NotNull ConcurrentList<String> emblemUnlocks = Concurrent.newList();
        @SerializedName("fishing_festival_sharks_killed")
        private int fishingFestivalSharksKilled;

        public double getLevel() {
            return Math.floor(this.getExperience() / 100.0);
        }

    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Quests implements PostInit {

        @Getter(AccessLevel.NONE)
        @SerializedName("harp_quest")
        private @NotNull ConcurrentMap<String, Object> melodyHarpMap = Concurrent.newMap();
        private transient MelodyHarp melodyHarp;
        @SerializedName("trapper_quest")
        private @NotNull Trapper trapper = new Trapper();

        @Override
        public void postInit() {
            this.melodyHarp = new MelodyHarp(this.melodyHarpMap);
        }

        @Getter
        public static class MelodyHarp {

            private final boolean talismanClaimed;
            private final @NotNull Optional<String> selectedSong;
            private final @NotNull SkyBlockDate.RealTime selectedSongTimestamp;
            private final @NotNull ConcurrentMap<String, MelodyHarp.Song> songs;

            MelodyHarp(@NotNull ConcurrentMap<String, Object> harpQuest) {
                this.talismanClaimed = (boolean) harpQuest.removeOrGet("claimed_talisman", false);
                this.selectedSong = harpQuest.getOptional("selected_song").map(String::valueOf);
                long epoch = NumberUtil.createNumber(String.valueOf(harpQuest.removeOrGet("selected_song_epoch", 0))).longValue();
                this.selectedSongTimestamp = new SkyBlockDate.RealTime(epoch * 1000);

                ConcurrentLinkedMap<String, ConcurrentMap<String, Integer>> songMap = Concurrent.newLinkedMap();
                harpQuest.stream()
                    .filterValue(Number.class::isInstance)
                    .forEach((key, value) -> {
                        String songKey = key.replace("song_", "");
                        String songName = songKey.replaceAll("_((best|perfect)_)?completions?", "");
                        String category = songKey.replace(String.format("%s_", songName), "");

                        if (!songMap.containsKey(songName))
                            songMap.put(songName, Concurrent.newMap());

                        songMap.get(songName).put(category, NumberUtil.createNumber(value.toString()).intValue());
                    });

                this.songs = songMap.stream()
                    .map((key, value) -> Pair.of(
                        key,
                        new MelodyHarp.Song(
                            value.getOrDefault("best_completion", 0),
                            value.getOrDefault("completions", 0),
                            value.getOrDefault("perfect_completions", 0)
                        )
                    ))
                    .collect(Concurrent.toUnmodifiableMap());
            }

            @Getter
            @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
            public static class Song {

                private final int bestCompletion;
                private final int completions;
                private final int perfectCompletions;

            }

        }

        @Getter
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Trapper {

            @SerializedName("last_task_time")
            private SkyBlockDate.RealTime lastTask;
            @SerializedName("pelt_count")
            private int peltCount;

        }

    }

    @Getter
    public static class TrophyFishing {

        private final @NotNull ConcurrentMap<String, ConcurrentMap<TrophyFish.Tier, Integer>> fish;
        private final int totalCaught;
        private final @NotNull PairOptional<String, TrophyFish.Tier> lastCaught;

        public TrophyFishing(@NotNull ConcurrentMap<String, Object> trophy_fish) {
            this.totalCaught = (int) trophy_fish.removeOrGet("total_caught", 0);

            this.lastCaught = PairOptional.of(
                trophy_fish.getOptional("last_caught")
                    .map(String::valueOf)
                    .map(value -> value.split("/"))
                    .map(parts -> Pair.of(parts[0], TrophyFish.Tier.valueOf(parts[1])))
            );

            this.fish = MinecraftApi.getRepository(TrophyFish.class)
                .stream()
                .map(type -> Pair.of(
                    type.getId(),
                    trophy_fish.stream()
                        .filter(entry -> entry.getKey().startsWith(type.getId().toLowerCase()))
                        .map(entry -> Pair.of(
                            TrophyFish.Tier.valueOf(entry.getKey().replace(type.getId(), "")),
                            (int) entry.getValue()
                        ))
                        .collect(Concurrent.toMap())
                ))
                .collect(Concurrent.toMap());
        }

    }
    
}
