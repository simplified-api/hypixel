package dev.sbs.minecraftapi.client.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.sbs.api.collection.concurrent.Concurrent;
import dev.sbs.api.collection.concurrent.ConcurrentList;
import dev.sbs.api.collection.concurrent.ConcurrentMap;
import dev.sbs.api.io.gson.PostInit;
import dev.sbs.api.io.gson.SerializedPath;
import dev.sbs.api.tuple.pair.Pair;
import dev.sbs.api.util.mutable.MutableDouble;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.*;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.attribute.AttributeProgress;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.crimson.CrimsonIsle;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.dungeon.DungeonProgress;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.hoppity.ChocolateFactory;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining.ForgeItem;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining.GlaciteTunnels;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.mining.MiningCore;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.pet.PetProgress;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.rift.RiftProgress;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.skill.SkillProgress;
import dev.sbs.minecraftapi.client.hypixel.response.skyblock.member.slayer.SlayerProgress;
import dev.sbs.minecraftapi.skyblock.common.Weight;
import dev.sbs.minecraftapi.skyblock.date.SkyBlockDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
    private @NotNull PlayerProgress progress = new PlayerProgress();
    @SerializedName("slayer")
    private @NotNull SlayerProgress slayers = new SlayerProgress();
    @SerializedName("pets_data")
    private @NotNull PetProgress pets = new PetProgress();
    @SerializedName("dungeons")
    private @NotNull DungeonProgress dungeons = new DungeonProgress();
    @SerializedName("rift")
    private @NotNull RiftProgress rift = new RiftProgress();
    @SerializedName("shards")
    private @NotNull AttributeProgress attributes = new AttributeProgress();
    @SerializedName("player_stats")
    private @NotNull Statistics statistics = new Statistics();
    private transient SkillProgress skills;

    // Core
    @SerializedName("foraging_core")
    private @NotNull ForagingCore foraging = new ForagingCore();

    @Getter(AccessLevel.NONE)
    private @NotNull Events events = new Events();

    // Mining
    @SerializedName("mining_core")
    private @NotNull MiningCore mining = new MiningCore();
    @SerializedPath("forge.forge_processes.forge_1")
    private @NotNull ConcurrentMap<Integer, ForgeItem> forge = Concurrent.newMap();
    @SerializedName("glacite_player_data")
    private @NotNull GlaciteTunnels glaciteTunnels = new GlaciteTunnels();

    private @NotNull Bestiary bestiary = new Bestiary();
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
    @SerializedName("shared_inventory")
    private @NotNull SharedInventory sharedInventory = new SharedInventory();
    private @NotNull Optional<Quests> quests = Optional.empty();
    @Getter(AccessLevel.NONE)
    private @NotNull Temples temples = new Temples();

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
        this.skills = new SkillProgress(this.getProgress().getSkillExperience(), this);

        this.collectionUnlocked = this.getCollection()
            .stream()
            .map((itemId, value) -> Pair.of(itemId, this.getProgress()
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
        return this.getProgress().getCraftedMinions(itemId);
    }

    public @NotNull ChocolateFactory getChocolateFactory() {
        return this.events.getChocolateFactory();
    }

    public @NotNull ConcurrentList<String> getUnlockedTemples() {
        return this.temples.getUnlockedTemples();
    }

    // Weight

    public @NotNull Weight getTotalWeight() {
        // Load Weights
        Weight skillWeight = this.getTotalWeight(member -> member.getSkills().getWeight());
        Weight slayerWeight = this.getTotalWeight(member -> member.getSlayers().getWeight());
        Weight dungeonWeight = this.getTotalWeight(member -> member.getDungeons().getWeight());
        Weight dungeonClassWeight = this.getTotalWeight(member -> member.getDungeons().getClassWeight());

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
    private static class Events {

        @SerializedName("easter")
        private @NotNull ChocolateFactory chocolateFactory = new ChocolateFactory();

    }

    @Getter
    private static class Temples {

        @SerializedName("unlocked_temples")
        private @NotNull ConcurrentList<String> unlockedTemples = Concurrent.newList();

    }

}
