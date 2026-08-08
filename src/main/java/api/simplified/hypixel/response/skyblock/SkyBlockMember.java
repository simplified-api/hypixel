package api.simplified.hypixel.response.skyblock;

import api.simplified.hypixel.common.Weight;
import api.simplified.hypixel.response.skyblock.member.*;
import api.simplified.hypixel.response.skyblock.member.attribute.AttributeShards;
import api.simplified.hypixel.response.skyblock.member.crimson.CrimsonIsle;
import api.simplified.hypixel.response.skyblock.member.crimson.TrophyFishing;
import api.simplified.hypixel.response.skyblock.member.dungeon.Dungeons;
import api.simplified.hypixel.response.skyblock.member.foraging.Foraging;
import api.simplified.hypixel.response.skyblock.member.foraging.HeartOfTheForest;
import api.simplified.hypixel.response.skyblock.member.hoppity.ChocolateFactory;
import api.simplified.hypixel.response.skyblock.member.mining.ForgeItem;
import api.simplified.hypixel.response.skyblock.member.mining.GlaciteTunnels;
import api.simplified.hypixel.response.skyblock.member.mining.HeartOfTheMountain;
import api.simplified.hypixel.response.skyblock.member.pet.Pets;
import api.simplified.hypixel.response.skyblock.member.rift.Rift;
import api.simplified.hypixel.response.skyblock.member.skill.Skills;
import api.simplified.hypixel.response.skyblock.member.slayer.Slayers;
import com.google.gson.annotations.SerializedName;
import dev.sbs.skyblockdata.date.SkyBlockDate;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.SerializedPath;
import dev.simplified.util.NumberUtil;
import dev.simplified.util.mutable.MutableDouble;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Getter
@NoArgsConstructor
public class SkyBlockMember {

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

    // Progression
    private @NotNull Leveling leveling = new Leveling();
    @SerializedName("skill_tree")
    private @NotNull SkillTree skillTree = new SkillTree();
    @SerializedName("player_data")
    private @NotNull PlayerData playerData = new PlayerData();
    private @NotNull Currencies currencies = new Currencies();
    @Getter(AccessLevel.NONE)
    private transient Skills skills;

    // Combat
    @SerializedName("slayer")
    private @NotNull Slayers slayers = new Slayers();
    @SerializedName("dungeons")
    private @NotNull Dungeons dungeons = new Dungeons();
    private @NotNull Bestiary bestiary = new Bestiary();

    // Pets
    @SerializedName("pets_data")
    private @NotNull Pets pets = new Pets();

    // Mining
    @SerializedName("mining_core")
    private @NotNull HeartOfTheMountain mining = new HeartOfTheMountain();
    @SerializedPath("forge.forge_processes.forge_1")
    private @NotNull ConcurrentMap<Integer, ForgeItem> forgeSlots = Concurrent.newMap();
    @SerializedName("glacite_player_data")
    private @NotNull GlaciteTunnels glaciteTunnels = new GlaciteTunnels();

    // Foraging
    private @NotNull Foraging foraging = new Foraging();
    @SerializedName("foraging_core")
    private @NotNull HeartOfTheForest heartOfTheForest = new HeartOfTheForest();
    @SerializedPath("temples.unlocked_temples")
    private @NotNull ConcurrentList<String> unlockedTemples = Concurrent.newList();

    // Crimson Isle
    @SerializedName("nether_island_player_data")
    private @NotNull CrimsonIsle crimsonIsle = new CrimsonIsle();
    @SerializedName("trophy_fish")
    private @NotNull TrophyFishing trophyFish = new TrophyFishing();

    // Rift
    @SerializedName("rift")
    private @NotNull Rift rift = new Rift();

    // Garden
    @SerializedName("garden_player_data")
    private @NotNull GardenCore garden = new GardenCore();
    @SerializedName("jacobs_contest")
    private @NotNull JacobsContest jacobsContest = new JacobsContest();
    @SerializedPath("quests.trapper_quest")
    private @NotNull Trapper trapper = new Trapper();

    // Events
    @SerializedPath("events.easter")
    private @NotNull ChocolateFactory chocolateFactory = new ChocolateFactory();
    @SerializedName("winter_player_data")
    private @NotNull WinterIsland jerrysWorkshop = new WinterIsland();
    private @NotNull Experimentation experimentation = new Experimentation();
    @SerializedName("fairy_soul")
    private @NotNull FairySouls fairySouls = new FairySouls();

    // Inventory
    private @NotNull Inventory inventory = new Inventory();
    @SerializedName("shared_inventory")
    private @NotNull SharedInventory sharedInventory = new SharedInventory();
    @Getter(AccessLevel.NONE)
    @SerializedName("accessory_bag_storage")
    private @NotNull AccessoryBag accessoryBag = new AccessoryBag();
    @SerializedName("item_data")
    private @NotNull ItemSettings itemSettings = new ItemSettings();
    @SerializedName("shards")
    private @NotNull AttributeShards attributes = new AttributeShards();
    @SerializedPath("attributes.stacks")
    private @NotNull ConcurrentMap<String, Integer> attributeStacks = Concurrent.newMap();
    @SerializedName("loadout")
    private @NotNull Loadouts loadouts = new Loadouts();

    // Collection
    private @NotNull ConcurrentMap<String, Long> collection = Concurrent.newMap();
    @Getter(AccessLevel.NONE)
    private transient ConcurrentMap<String, Integer> collectionUnlocked;

    // Statistics
    @SerializedName("player_stats")
    private @NotNull Statistics statistics = new Statistics();

    // Miscellaneous
    @SerializedName("objectives")
    @Capture(grouping = Capture.Grouping.ENTRY, descend = true)
    private @NotNull ConcurrentMap<String, Objective> objectives = Concurrent.newMap();
    @Extract("objectives.tutorial")
    private @NotNull ConcurrentList<String> tutorialObjectives = Concurrent.newList();

    /**
     * Accessory bag, wired with the three member-scoped values it cannot reach from its own node
     */
    public @NotNull AccessoryBag getAccessoryBag() {
        return this.accessoryBag.initialize(
            this.getInventory().getBags().getAccessories(),
            this.getRift().getAccess().hasConsumedPrism(),
            this.getCrimsonIsle().getAbiphone().getContacts().size()
        );
    }

    /**
     * Skill levels derived from this member's skill experience
     */
    public @NotNull Skills getSkills() {
        if (this.skills == null)
            this.skills = new Skills(this.getPlayerData().getSkillExperience(), this);

        return this.skills;
    }

    /**
     * Highest unlocked collection tier per collected item id, defaulting to zero when the item is
     * collected but no tier is claimed
     * <p>
     * One pass over the tier strings rather than one regex per collected item, splitting each at its
     * last underscore - the item id may itself carry underscores and colons.
     */
    public @NotNull ConcurrentMap<String, Integer> getCollectionUnlocked() {
        if (this.collectionUnlocked == null) {
            ConcurrentMap<String, Integer> highestTiers = Concurrent.newMap();

            for (String unlocked : this.getPlayerData().getUnlockedCollectionTiers()) {
                int split = unlocked.lastIndexOf('_');
                if (split < 0) continue;

                String itemId = unlocked.substring(0, split);
                Integer tier = NumberUtil.tryParseInt(unlocked.substring(split + 1));

                // a negative tier marks a collection that is visible with nothing claimed, which is
                // tier zero - every id carrying one also carries its positive tiers
                if (tier == null || tier < 0 || !this.getCollection().containsKey(itemId)) continue;

                highestTiers.merge(itemId, tier, Math::max);
            }

            this.getCollection().forEach((itemId, collected) -> highestTiers.putIfAbsent(itemId, 0));
            this.collectionUnlocked = highestTiers.toUnmodifiable();
        }

        return this.collectionUnlocked;
    }

    public @NotNull ConcurrentList<Integer> getCraftedMinions(@NotNull String itemId) {
        return this.getPlayerData().getCraftedMinions(itemId);
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

}
