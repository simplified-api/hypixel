package api.simplified.hypixel.response.skyblock;

import api.simplified.hypixel.response.skyblock.member.AccessoryBag;
import api.simplified.hypixel.response.skyblock.member.Bestiary;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.simplified.gson.GsonSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
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

    /**
     * Snapshot of {@link #populated} taken before any decode runs.
     * <p>
     * A {@code @Lenient} decode rewrites the caller's own tree - gson hands a
     * {@code JsonTreeReader} the live element rather than a copy, so the filter phase's
     * {@code replaceElement} strips overflow entries out of the fixture itself. Tests that need
     * the untouched wire shape read it from here and decode from a fresh copy.
     */
    private static JsonObject pristine;

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
            pristine = populated.deepCopy();
        }
    }

    private static JsonObject rawPristine(String key) {
        return pristine.getAsJsonObject(key);
    }

    private static <T> T decodePristine(String key, Class<T> type) {
        return gson.fromJson(pristine.get(key).deepCopy(), type);
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
        assertThat(populatedBag.getTuning().getSlot(0).orElseThrow().getStats(), hasKey("critical_damage"));
        assertThat(populatedBag.getTuning().getSlot(0).orElseThrow().getStats().get("critical_damage"), is(equalTo(211)));
        assertThat(populatedBag.getTuning().getSlot(0).orElseThrow().getStats(), not(hasKey("purchase_ts")));
        assertThat(sparseBag.getTuning().hasClaimedSecondRefund(), is(true));
        assertThat(sparseBag.getTuning().getSlot(1).orElseThrow().getPurchased().isPresent(), is(true));
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
    @DisplayName("candy collected decodes one festival per key outside its named fields")
    void mapsCandyFestivals() {
        Statistics statistics = this.decode(populated, "player_stats", Statistics.class);
        // read the expectation from the pristine snapshot: a decode can rewrite the caller's tree
        // in place, and deriving both sides of the comparison from the same live tree would let a
        // loss cancel itself out instead of failing
        JsonObject raw = rawPristine("player_stats").getAsJsonObject("candy_collected");

        // every raw key other than the three declared fields is a festival
        int expected = raw.size() - 3;

        assertThat(statistics.getCandy().getFestivals().size(), is(equalTo(expected)));
        assertThat(statistics.getCandy().getTotal(), is(equalTo(raw.get("total").getAsInt())));
        assertThat(statistics.getCandy().getFestivals(), hasKey("spooky_festival_1"));
        assertThat(statistics.getCandy().getFestivals().get("spooky_festival_1").getTotal(),
            is(equalTo(raw.getAsJsonObject("spooky_festival_1").get("total").getAsInt())));
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
    @DisplayName("a @Lenient decode strips overflow entries out of the caller's own tree")
    void lenientDecodeRewritesCallerTree() {
        JsonObject own = rawPristine("loadout").deepCopy();

        assertThat(own.getAsJsonObject("armor").has("equipped_set"), is(true));

        gson.fromJson(own, Loadouts.class);

        // gson hands a JsonTreeReader the live element, so the filter phase's replaceElement
        // rewrites the caller's object - the overflowed key is gone once the decode returns
        assertThat(own.getAsJsonObject("armor").has("equipped_set"), is(false));
        assertThat(own.getAsJsonObject("equipment").has("equipped_set"), is(false));
    }

    @Test
    @DisplayName("loadout round-trips both equipped set ids back into their own sub-objects")
    void roundTripsLoadouts() {
        JsonObject raw = rawPristine("loadout");
        Loadouts first = decodePristine("loadout", Loadouts.class);

        int rawArmorEquipped = raw.getAsJsonObject("armor").get("equipped_set").getAsInt();
        int rawEquipmentEquipped = raw.getAsJsonObject("equipment").get("equipped_set").getAsInt();

        assertThat(first.getEquippedArmorSet().orElseThrow(), is(equalTo(rawArmorEquipped)));
        assertThat(first.getEquippedEquipmentSet().orElseThrow(), is(equalTo(rawEquipmentEquipped)));

        JsonObject out = JsonParser.parseString(gson.toJson(first)).getAsJsonObject();

        // each claim goes back into its own source's sub-object, keyed as the wire spelled it
        assertThat(out.getAsJsonObject("armor").get("equipped_set").getAsInt(), is(equalTo(rawArmorEquipped)));
        assertThat(out.getAsJsonObject("equipment").get("equipped_set").getAsInt(), is(equalTo(rawEquipmentEquipped)));
        // the third @Lenient field carries no @Extract, and every one of its wire entries is
        // compatible, so it has no overflow at all and must gain nothing
        assertThat(out.getAsJsonObject("loadouts").has("equipped_set"), is(false));
        assertThat(out.getAsJsonObject("loadouts").keySet(),
            is(equalTo(raw.getAsJsonObject("loadouts").keySet())));
        assertThat(out.getAsJsonObject("armor").has("1"), is(true));

        // `out` currently also carries root-level equippedArmorSet/equippedEquipmentSet, and
        // neither field has a @SerializedName, so the reflective binder would set both Optionals
        // straight from those root keys. Strip them, or the re-decode below proves nothing about
        // the @Extract claim; once they are gone these removals are a no-op
        out.remove("equippedArmorSet");
        out.remove("equippedEquipmentSet");

        Loadouts second = gson.fromJson(out, Loadouts.class);

        assertThat(second.getEquippedArmorSet().orElseThrow(), is(equalTo(rawArmorEquipped)));
        assertThat(second.getEquippedEquipmentSet().orElseThrow(), is(equalTo(rawEquipmentEquipped)));
        assertThat(second.getArmorSets(), hasKey(1));
        assertThat(second.getEquipmentSets(), hasKey(1));
        assertThat(second.getLoadout(1).orElseThrow().getName(),
            is(equalTo(first.getLoadout(1).orElseThrow().getName())));
    }

    @Test
    @Disabled("""
        Currently red - "Expected: is <[armor, equipment, loadouts]> but: was \
        <[armor, equippedArmorSet, equipment, equippedEquipmentSet, loadouts]>". @Extract never \
        removes its own field's serialized key, so both sites here emit their extracted value \
        twice on every serialize - once inside armor/equipment and once at the root under the \
        Java field name, a key the input never carried. Enable once the pinned gson-extras \
        removes the root key.""")
    @DisplayName("loadout serialization emits no root-level @Extract keys")
    void loadoutsSerializeHasNoRootExtractKeys() {
        Loadouts loadouts = decodePristine("loadout", Loadouts.class);
        JsonObject out = JsonParser.parseString(gson.toJson(loadouts)).getAsJsonObject();

        assertThat(out.keySet(), is(equalTo(rawPristine("loadout").keySet())));
        assertThat(out.has("equippedArmorSet"), is(false));
        assertThat(out.has("equippedEquipmentSet"), is(false));
    }

    @Test
    @DisplayName("bestiary extracts the last killed mob and returns it to kills on write")
    void mapsBestiary() {
        JsonObject raw = rawPristine("bestiary");
        Bestiary bestiary = decodePristine("bestiary", Bestiary.class);

        String rawLastKilled = raw.getAsJsonObject("kills").get("last_killed_mob").getAsString();

        // the entry reaches overflow because its VALUE is a String, not because of the key
        assertThat(bestiary.getLastKilledMob().orElseThrow(), is(equalTo(rawLastKilled)));
        assertThat(bestiary.getKills(), not(hasKey("last_killed_mob")));
        assertThat(bestiary.getKills().size(), is(equalTo(raw.getAsJsonObject("kills").size() - 1)));
        // deaths is a second @Lenient field on the same class whose overflow stays empty
        assertThat(bestiary.getDeaths().size(), is(equalTo(raw.getAsJsonObject("deaths").size())));
        assertThat(bestiary.getLastClaimedMilestone(),
            is(equalTo(raw.getAsJsonObject("milestone").get("last_claimed_milestone").getAsInt())));

        JsonObject out = JsonParser.parseString(gson.toJson(bestiary)).getAsJsonObject();

        assertThat(out.getAsJsonObject("kills").get("last_killed_mob").getAsString(), is(equalTo(rawLastKilled)));
        assertThat(out.getAsJsonObject("kills").size(), is(equalTo(raw.getAsJsonObject("kills").size())));
        assertThat(out.getAsJsonObject("deaths").size(), is(equalTo(raw.getAsJsonObject("deaths").size())));
    }

    @Test
    @DisplayName("dungeon journal overflows entirely and is restored verbatim on write")
    void mapsDungeonsUnlockedJournals() {
        JsonArray raw = rawPristine("dungeons")
            .getAsJsonObject("dungeon_journal")
            .getAsJsonArray("unlocked_journals");
        Dungeons dungeons = decodePristine("dungeons", Dungeons.class);

        // the only collection-shaped @Lenient field in the workspace, so the JsonArray half of
        // the factory has single-site coverage and this is the site. It also drives the
        // @SerializedPath branch of locateElement/replaceElement, which it shares with exactly one
        // other field, Statistics.spawnedSpookyBats. Every wire entry is a String against a
        // declared ConcurrentList<Integer>, so the field binds empty and the whole list is overflow
        assertThat(raw.isEmpty(), is(false));
        assertThat(dungeons.getUnlockedJournals(), is(empty()));

        JsonObject out = JsonParser.parseString(gson.toJson(dungeons)).getAsJsonObject();
        JsonArray outJournals = out.getAsJsonObject("dungeon_journal").getAsJsonArray("unlocked_journals");

        assertThat(outJournals, is(equalTo(raw)));
        assertThat(out.has("unlockedJournals"), is(false));
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
