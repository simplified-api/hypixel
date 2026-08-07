package api.simplified.hypixel.response.skyblock;

import api.simplified.hypixel.response.skyblock.member.AccessoryBag;
import api.simplified.hypixel.response.skyblock.member.GardenCore;
import api.simplified.hypixel.response.skyblock.member.Loadouts;
import api.simplified.hypixel.response.skyblock.member.SkillTree;
import api.simplified.hypixel.response.skyblock.member.Statistics;
import api.simplified.hypixel.response.skyblock.member.Toolkit;
import api.simplified.hypixel.response.skyblock.member.WinterIsland;
import api.simplified.hypixel.response.skyblock.member.crimson.BoardQuest;
import api.simplified.hypixel.response.skyblock.member.crimson.CrimsonIsle;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonClass;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonData;
import api.simplified.hypixel.response.skyblock.member.dungeon.Dungeons;
import api.simplified.hypixel.response.skyblock.member.foraging.Foraging;
import api.simplified.hypixel.response.skyblock.member.foraging.HeartOfTheForest;
import api.simplified.hypixel.response.skyblock.member.hoppity.ChocolateFactory;
import api.simplified.hypixel.response.skyblock.member.mining.HeartOfTheMountain;
import api.simplified.hypixel.response.skyblock.member.pet.OwnedPet;
import api.simplified.hypixel.response.skyblock.member.rift.Rift;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.simplified.gson.GsonSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Verifies the member DTO field mappings against the bundled API response.
 * <p>
 * Subtrees are decoded individually because a whole {@link SkyBlockMember} runs
 * {@code postInit} against the SkyBlock model repositories, which need a live JPA
 * session this test deliberately does not stand up.
 */
class MemberDtoMappingTest {

    private static Gson gson;
    private static JsonObject sparse;
    private static JsonObject populated;

    @BeforeAll
    static void loadFixture() throws Exception {
        gson = GsonSettings.defaults().create();

        try (InputStream stream = MemberDtoMappingTest.class.getResourceAsStream("/craftedfury.json")) {
            if (stream == null)
                throw new IllegalStateException("craftedfury.json is missing from the classpath");

            JsonObject root = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
            ).getAsJsonObject();

            sparse = firstMember(root, 0);
            populated = firstMember(root, 1);
        }
    }

    private static JsonObject firstMember(JsonObject root, int profileIndex) {
        JsonObject members = root.getAsJsonArray("profiles")
            .get(profileIndex)
            .getAsJsonObject()
            .getAsJsonObject("members");

        return members.entrySet()
            .iterator()
            .next()
            .getValue()
            .getAsJsonObject();
    }

    private <T> T decode(JsonObject member, String key, Class<T> type) {
        return gson.fromJson(member.get(key), type);
    }

    @Test
    @DisplayName("dungeons exposes secrets, last run and hub race settings")
    void mapsDungeons() {
        Dungeons dungeons = this.decode(populated, "dungeons", Dungeons.class);

        assertThat(dungeons.getSecrets(), is(equalTo(15820)));
        assertThat(dungeons.getLastRun().orElseThrow(), is(equalTo("CATACOMBS_FLOOR_SEVEN")));
        assertThat(dungeons.getRaceSettings().getSelectedRace().orElseThrow(), is(equalTo("giant_mushroom")));
        assertThat(dungeons.getRaceSettings().getSelectedSetting().orElseThrow(), is(equalTo("anything")));
    }

    @Test
    @DisplayName("dungeon and class lookups fall back instead of throwing")
    void mapsDungeonLookups() {
        Dungeons dungeons = this.decode(populated, "dungeons", Dungeons.class);

        assertThat(dungeons.getDungeon(DungeonData.Type.CATACOMBS).getLevel(), is(not(equalTo(0))));
        assertThat(dungeons.getDungeon(DungeonData.Type.UNKNOWN).getExperience(), is(equalTo(0.0)));
        assertThat(dungeons.getClass(DungeonClass.Type.UNKNOWN).getExperience(), is(equalTo(0.0)));
    }

    @Test
    @DisplayName("accessory bag tuning captures every slot including slot_0")
    void mapsTuningSlots() {
        AccessoryBag populatedBag = this.decode(populated, "accessory_bag_storage", AccessoryBag.class);
        AccessoryBag sparseBag = this.decode(sparse, "accessory_bag_storage", AccessoryBag.class);

        assertThat(populatedBag.getTuning().getSlots(), hasKey(0));
        assertThat(populatedBag.getTuning().getSlotStats(0), hasKey("critical_damage"));
        assertThat(populatedBag.getTuning().getSlotStats(0).get("critical_damage"), is(equalTo(211L)));
        assertThat(populatedBag.getTuning().getSlotStats(0), not(hasKey("purchase_ts")));
        assertThat(sparseBag.getTuning().hasClaimedSecondRefund(), is(true));
        assertThat(sparseBag.getTuning().getSlotPurchased(1).isPresent(), is(true));
    }

    @Test
    @DisplayName("loadout decodes armor sets, equipment sets and named loadouts")
    void mapsLoadouts() {
        Loadouts loadouts = this.decode(populated, "loadout", Loadouts.class);

        assertThat(loadouts.getArmorSets(), hasKey(1));
        assertThat(loadouts.getArmorSets().get(1).getBoots().getRawData().isEmpty(), is(false));
        assertThat(loadouts.getEquipmentSets().get(1).getNecklace().getRawData().isEmpty(), is(false));
        assertThat(loadouts.getEquippedArmorSet().orElseThrow(), is(equalTo(10)));
        assertThat(loadouts.getLoadout(1).orElseThrow().getName(), is(equalTo("Dungeons LCM")));
        assertThat(loadouts.getLoadout(1).orElseThrow().getPowerStone().orElseThrow(), is(equalTo("silky")));
    }

    @Test
    @DisplayName("garden toolkit decodes unlock flag, tool items and in-use slots")
    void mapsFarmingToolkit() {
        GardenCore garden = this.decode(populated, "garden_player_data", GardenCore.class);
        Toolkit toolkit = garden.getFarmingToolkit();

        assertThat(toolkit.isUnlocked(), is(true));
        assertThat(toolkit.getTools(), hasKey("SUGAR_CANE"));
        assertThat(toolkit.getTool("SUGAR_CANE").isEmpty(), is(false));
        assertThat(toolkit.getTool("SUGAR_CANE").getFirst().getRawData().isEmpty(), is(false));
        assertThat(toolkit.isInUse("SUGAR_CANE", 0), is(false));

        // the two explicitly named keys must not leak into the captured tool map
        assertThat(toolkit.getTools(), not(hasKey("IS_UNLOCKED")));
        assertThat(toolkit.getTools(), not(hasKey("IN_USE")));
    }

    @Test
    @DisplayName("hunting toolkit decodes tool items and in-use slots")
    void mapsHuntingToolkit() {
        Foraging foraging = this.decode(populated, "foraging", Foraging.class);
        Toolkit toolkit = foraging.getHuntingToolkit();

        assertThat(toolkit.getTools(), hasKey("HUNTING_SCYTHE"));
        assertThat(toolkit.getTool("TRAP").isEmpty(), is(false));
        assertThat(toolkit.isInUse("LASSO", 0), is(true));
        assertThat(toolkit.isInUse("HUNTING_SCYTHE", 0), is(false));
        assertThat(toolkit.getTools(), not(hasKey("IN_USE")));
    }

    @Test
    @DisplayName("heart of the forest decodes per-biome whisper tiers")
    void mapsForestWhispers() {
        HeartOfTheForest forest = this.decode(populated, "foraging_core", HeartOfTheForest.class);
        JsonObject rawForest = populated.getAsJsonObject("foraging_core")
            .getAsJsonObject("whispers")
            .getAsJsonObject("forest");

        assertThat(forest.getBiomeWhispers(), hasKey("forest"));
        assertThat(forest.getBiomeWhispers(), hasKey("desert"));
        assertThat(forest.getBiomeWhispers().get("forest").getTiers(), hasKey(1));
        assertThat(forest.getBiomeWhispers().get("forest").getSpent(1),
            is(equalTo(rawForest.getAsJsonObject("1").get("spent").getAsInt())));
        assertThat(forest.getBiomeWhispers().get("forest").getTotal(),
            is(equalTo(rawForest.get("total").getAsInt())));
    }

    @Test
    @DisplayName("skill tree decodes the selected slot and free trial day")
    void mapsSkillTree() {
        SkillTree skillTree = this.decode(populated, "skill_tree", SkillTree.class);

        assertThat(skillTree.getLastFreeTrialDay(), is(equalTo(66)));
        assertThat(skillTree.getSelectedSlot(), hasKey("foraging"));
    }

    @Test
    @DisplayName("statistics decode shard hunts and the corrected dragon fight keys")
    void mapsStatistics() {
        Statistics statistics = this.decode(populated, "player_stats", Statistics.class);

        assertThat(statistics.getUniqueShards(), is(equalTo(96)));
        assertThat(statistics.getCombatShardHunts(), is(equalTo(2926)));
        assertThat(statistics.getSaltShardHunts(), is(equalTo(302)));
        assertThat(statistics.getItemsFished().getOutstanding(), is(equalTo(2)));
        assertThat(statistics.getItemsFished().getTrophyFrog(), is(equalTo(263)));
        assertThat(statistics.getEndIsland().getDragonFight().getEnderCrystalsDestroyed(), is(equalTo(168)));
        assertThat(statistics.getEndIsland().getDragonFight().getAmountSummoned().isEmpty(), is(false));
    }

    @Test
    @DisplayName("chocolate factory decodes level and the corrected hotspot key")
    void mapsChocolateFactory() {
        JsonObject easter = populated.getAsJsonObject("events").getAsJsonObject("easter");
        ChocolateFactory factory = gson.fromJson(easter, ChocolateFactory.class);

        assertThat(factory.getChocolateLevel(), is(equalTo(easter.get("chocolate_level").getAsInt())));
        assertThat(factory.getChocolateLevel(), is(not(equalTo(0))));
        assertThat(factory.getRabbitHotspot(), is(equalTo(easter.get("rabbit_hotspot_filer").getAsString())));
    }

    @Test
    @DisplayName("crimson isle decodes highest reputation, board quests and contact items")
    void mapsCrimsonIsle() {
        CrimsonIsle crimsonIsle = this.decode(populated, "nether_island_player_data", CrimsonIsle.class);

        assertThat(crimsonIsle.getHighestBarbarianReputation(), is(equalTo(12730)));
        assertThat(crimsonIsle.getQuests().getQuestBoard().getFishingQuest().getStatus(),
            is(equalTo(BoardQuest.Status.ACTIVE)));
        assertThat(crimsonIsle.getQuests().getQuestBoard().getMiniBossQuest().getStatus(),
            is(equalTo(BoardQuest.Status.ACTIVE)));
        assertThat(crimsonIsle.getQuests().getQuestBoard().getQuestList().isEmpty(), is(false));
        assertThat(crimsonIsle.getAbiphone().getContacts().get("dalir").hasGivenItems(), is(true));
    }

    @Test
    @DisplayName("rift slayer quest decodes the type key as its id")
    void mapsRiftSlayerQuest() {
        Rift rift = this.decode(populated, "rift", Rift.class);

        assertThat(rift.getSlayerQuest().getId(), is(equalTo("vampire")));
        assertThat(rift.getSlayerQuest().getTier(), is(equalTo(3)));
        assertThat(rift.getInventory().getEnderChestPageIcons().size(), is(equalTo(9)));
    }

    @Test
    @DisplayName("winter island and mining core decode their remaining flags")
    void mapsMiscellaneous() {
        WinterIsland winter = this.decode(populated, "winter_player_data", WinterIsland.class);
        HeartOfTheMountain mining = this.decode(sparse, "mining_core", HeartOfTheMountain.class);

        assertThat(winter.getRefinedJyrreUses(), is(equalTo(5)));
        assertThat(mining.hasPendingTreeResetMessage(), is(true));
    }

    @Test
    @DisplayName("owned pets decode the held item uuid")
    void mapsHeldItemUniqueId() {
        boolean anyHeldItemUniqueId = false;

        for (var element : populated.getAsJsonObject("pets_data").getAsJsonArray("pets")) {
            OwnedPet pet = gson.fromJson(element, OwnedPet.class);

            if (pet.getHeldItemUniqueId().isPresent()) {
                anyHeldItemUniqueId = true;
                break;
            }
        }

        assertThat(anyHeldItemUniqueId, is(true));
    }

    @Test
    @DisplayName("attribute stacks decode from the member attributes object")
    void mapsAttributeStacks() {
        JsonObject stacks = populated.getAsJsonObject("attributes").getAsJsonObject("stacks");
        Map<?, ?> decoded = gson.fromJson(stacks, Map.class);

        assertThat(decoded.isEmpty(), is(false));
        assertThat(decoded, hasKey("magic_find"));
    }

}
