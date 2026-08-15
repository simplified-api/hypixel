package api.simplified.hypixel.response.skyblock;

import api.simplified.hypixel.common.IdTiers;
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
import api.simplified.skyblock.date.SkyBlockDate;
import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;
import dev.simplified.annotations.NoArgsConstructor;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.annotation.Capture;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.SerializedPath;
import dev.simplified.util.mutable.MutableDouble;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * One player's entire state on one profile, and the root of the member response tree.
 * <p>
 * Almost every field here is a subtree rather than a value, because the wire groups a member's state
 * by the part of the game it came from. Each one defaults to an empty instance rather than to null:
 * a subtree Hypixel omits is the player's API privacy setting rather than an error, so absence has to
 * reach a caller as an empty object and never as a null.
 * <p>
 * Binding this derives nothing and opens no session. The handful of accessors below that do derive
 * are the exceptions, and each says so.
 *
 * @see <a href="https://hypixelskyblock.minecraft.wiki/w/Profile">Profile</a>
 */
@Getter
@NoArgsConstructor
public class SkyBlockMember {

    // Profile

    /**
     * The player this member is, identifying them across every profile they own.
     */
    @SerializedName("player_id")
    private @NotNull UUID uniqueId;

    /**
     * When the player first reached the hub, in SkyBlock's own calendar.
     */
    @SerializedName("first_join_hub")
    private SkyBlockDate.SkyBlockTime firstJoinHub;

    /**
     * When the player first joined this profile.
     */
    @SerializedPath("profile.first_join")
    private SkyBlockDate.RealTime firstJoin;

    /**
     * Tier the player has bought for their personal bank, raising its interest and its cap.
     */
    @SerializedPath("profile.personal_bank_upgrade")
    private int personalBankUpgrade;

    /**
     * Whether a booster cookie is currently running, granting bits and the cookie buff.
     */
    @SerializedPath("profile.cookie_buff_active")
    private boolean boosterCookieActive;

    // Progression

    /**
     * SkyBlock levels, the profile-wide progression earned from tasks across the whole game.
     */
    private @NotNull Leveling leveling = new Leveling();

    /**
     * Skill tree nodes, which now carry the perk levels and spent tokens for mining and foraging.
     */
    @SerializedName("skill_tree")
    private @NotNull SkillTree skillTree = new SkillTree();

    /**
     * Loose per-player state, including the skill experience every skill level is derived from.
     */
    @SerializedName("player_data")
    private @NotNull PlayerData playerData = new PlayerData();

    /**
     * Coins in the purse, motes, and the essence of each type.
     */
    private @NotNull Currencies currencies = new Currencies();

    @Getter(AccessLevel.NONE)
    private transient Skills skills;

    // Combat

    /**
     * Slayer experience and quest state for every boss.
     */
    @SerializedName("slayer")
    private @NotNull Slayers slayers = new Slayers();

    /**
     * Catacombs progress, covering the floors, the classes and the dungeon journal.
     */
    @SerializedName("dungeons")
    private @NotNull Dungeons dungeons = new Dungeons();

    /**
     * Kills recorded against each mob, which the bestiary families are counted from.
     */
    private @NotNull Bestiary bestiary = new Bestiary();

    // Pets

    /**
     * Every pet owned, along with the autopet rules and pet care state.
     */
    @SerializedName("pets_data")
    private @NotNull Pets pets = new Pets();

    /**
     * Heart of the Mountain, holding the powder totals and the Crystal Hollows state.
     */
    @SerializedName("mining_core")
    private @NotNull HeartOfTheMountain mining = new HeartOfTheMountain();

    /**
     * The Dwarven Forge slots and what each is currently smelting, keyed by slot.
     */
    @SerializedPath("forge.forge_processes.forge_1")
    private @NotNull ConcurrentMap<Integer, ForgeItem> forgeSlots = Concurrent.newMap();

    /**
     * Glacite Tunnels progress, including the fossils excavated and the corpses looted.
     */
    @SerializedName("glacite_player_data")
    private @NotNull GlaciteTunnels glaciteTunnels = new GlaciteTunnels();

    // Foraging

    /**
     * Foraging state, covering the trees felled and Melody's Harp.
     */
    private @NotNull Foraging foraging = new Foraging();

    /**
     * Heart of the Forest, the foraging counterpart to Heart of the Mountain.
     */
    @SerializedName("foraging_core")
    private @NotNull HeartOfTheForest heartOfTheForest = new HeartOfTheForest();

    /**
     * Ids of the temples the player has unlocked.
     */
    @SerializedPath("temples.unlocked_temples")
    private @NotNull ConcurrentList<String> unlockedTemples = Concurrent.newList();

    // Crimson Isle

    /**
     * Crimson Isle state, covering faction reputation, Kuudra, the Dojo and the quest board.
     */
    @SerializedName("nether_island_player_data")
    private @NotNull CrimsonIsle crimsonIsle = new CrimsonIsle();

    /**
     * Trophy fish caught, counted per fish and per tier.
     */
    @SerializedName("trophy_fish")
    private @NotNull TrophyFishing trophyFish = new TrophyFishing();

    // Rift

    /**
     * Rift progress, which is tracked separately from the rest of the profile.
     */
    @SerializedName("rift")
    private @NotNull Rift rift = new Rift();

    // Garden

    /**
     * Garden state carried on the member rather than on the shared garden itself.
     */
    @SerializedName("garden_player_data")
    private @NotNull GardenCore garden = new GardenCore();

    /**
     * Jacob's farming contests entered, along with the perks and medals earned.
     */
    @SerializedName("jacobs_contest")
    private @NotNull JacobsContest jacobsContest = new JacobsContest();

    /**
     * The trapper quest currently accepted, which the bestiary trapping loop runs through.
     */
    @SerializedPath("quests.trapper_quest")
    private @NotNull Trapper trapper = new Trapper();

    // Events

    /**
     * Chocolate Factory state from Hoppity's Hunt, kept under the easter event node.
     */
    @SerializedPath("events.easter")
    private @NotNull ChocolateFactory chocolateFactory = new ChocolateFactory();

    /**
     * Jerry's Workshop progress from the winter event.
     */
    @SerializedName("winter_player_data")
    private @NotNull WinterIsland jerrysWorkshop = new WinterIsland();

    /**
     * Experimentation Table results, one entry per minigame played.
     */
    private @NotNull Experimentation experimentation = new Experimentation();

    /**
     * Fairy souls found and exchanged, and the fairy exchanges taken.
     */
    @SerializedName("fairy_soul")
    private @NotNull FairySouls fairySouls = new FairySouls();

    // Inventory

    /**
     * Every container the player carries, each held as base64 NBT rather than as readable JSON.
     */
    private @NotNull Inventory inventory = new Inventory();

    /**
     * The containers shared across every profile the player owns.
     */
    @SerializedName("shared_inventory")
    private @NotNull SharedInventory sharedInventory = new SharedInventory();

    @Getter(AccessLevel.NONE)
    @SerializedName("accessory_bag_storage")
    private @NotNull AccessoryBag accessoryBag = new AccessoryBag();

    /**
     * Per-item display and pickup settings the player has chosen.
     */
    @SerializedName("item_data")
    private @NotNull ItemSettings itemSettings = new ItemSettings();

    /**
     * Attribute shards absorbed, the foraging-era replacement for attribute levelling.
     */
    @SerializedName("shards")
    private @NotNull AttributeShards attributes = new AttributeShards();

    /**
     * Attribute stacks currently built up, keyed by attribute id.
     */
    @SerializedPath("attributes.stacks")
    private @NotNull ConcurrentMap<String, Integer> attributeStacks = Concurrent.newMap();

    /**
     * Saved equipment loadouts the player can switch between.
     */
    @SerializedName("loadout")
    private @NotNull Loadouts loadouts = new Loadouts();

    // Collection

    /**
     * Lifetime amount gathered of each collection item, keyed by the item id.
     * <p>
     * A collection id may carry a colon, and the two halves are different collections - {@code
     * INK_SACK:3} is not {@code INK_SACK}.
     */
    private @NotNull ConcurrentMap<String, Long> collection = Concurrent.newMap();

    @Getter(AccessLevel.NONE)
    private transient ConcurrentMap<String, Integer> collectionUnlocked;

    // Statistics

    /**
     * The counters Hypixel keeps for the member, from damage dealt to races run.
     */
    @SerializedName("player_stats")
    private @NotNull Statistics statistics = new Statistics();

    // Miscellaneous

    /**
     * Every objective and quest the member has been given, keyed by its id.
     */
    @SerializedName("objectives")
    @Capture(grouping = Capture.Grouping.ENTRY, descend = true)
    private @NotNull ConcurrentMap<String, Objective> objectives = Concurrent.newMap();

    /**
     * Tutorial steps completed, lifted out of the objectives node rather than bound from a root key.
     */
    @Extract("objectives.tutorial")
    private @NotNull ConcurrentList<String> tutorialObjectives = Concurrent.newList();

    /**
     * Accessory bag, wired with the three member-scoped values it cannot reach from its own node.
     * <p>
     * The talisman bag's contents, the consumed rift prism and the abiphone contact count all live
     * elsewhere on the member, so they are handed over on every call. A bag decoded on its own is
     * never wired and reads empty rather than throwing.
     */
    public @NotNull AccessoryBag getAccessoryBag() {
        return this.accessoryBag.initialize(
            this.getInventory().getBags().getAccessories(),
            this.getRift().getAccess().hasConsumedPrism(),
            this.getCrimsonIsle().getAbiphone().getContacts().size()
        );
    }

    /**
     * Skill levels derived from this member's skill experience, computed once and reused.
     * <p>
     * There is no skills node on the wire - the levels are built from the experience totals under
     * the player data, which is why this derives rather than binds.
     */
    public @NotNull Skills getSkills() {
        if (this.skills == null)
            this.skills = new Skills(this.getPlayerData().getSkillExperience(), this);

        return this.skills;
    }

    /**
     * Highest unlocked collection tier per collected item id, defaulting to zero when the item is
     * collected but no tier is claimed.
     * <p>
     * One pass over the tier strings rather than one regex per collected item, splitting each at its
     * last underscore - the item id may itself carry underscores and colons. Computed once, and the
     * same unmodifiable map is returned thereafter.
     */
    public @NotNull ConcurrentMap<String, Integer> getCollectionUnlocked() {
        if (this.collectionUnlocked == null) {
            ConcurrentMap<String, ConcurrentList<Integer>> unlocked = IdTiers.group(
                this.getPlayerData().getUnlockedCollectionTiers()
            );
            ConcurrentMap<String, Integer> highestTiers = Concurrent.newMap();

            this.getCollection().forEach((itemId, collected) -> highestTiers.put(
                itemId,
                unlocked.getOrDefault(itemId, Concurrent.newList())
                    .stream()
                    // a negative tier marks a collection that is visible with nothing claimed, which
                    // is tier zero - every id carrying one also carries its positive tiers
                    .filter(tier -> tier >= 0)
                    .max(Comparator.naturalOrder())
                    .orElse(0)
            ));

            this.collectionUnlocked = highestTiers.toUnmodifiable();
        }

        return this.collectionUnlocked;
    }

    /**
     * Reads the minion tiers crafted for one minion type.
     *
     * @param itemId the minion's item id
     * @return the tiers crafted, empty when the minion has never been crafted
     */
    public @NotNull ConcurrentList<Integer> getCraftedMinions(@NotNull String itemId) {
        return this.getPlayerData().getCraftedMinions(itemId);
    }

    // Weight

    /**
     * The member's whole weight, summing the skills, the slayers, the dungeons and the dungeon
     * classes.
     * <p>
     * The capped and overflow halves are summed separately and stay separate, so a member who has
     * maxed a progression can still be told apart from one who has only just reached the cap.
     */
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
