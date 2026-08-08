package api.simplified.hypixel.response.skyblock;

import api.simplified.hypixel.response.hypixel.HypixelPlayer;
import api.simplified.hypixel.response.skyblock.election.Election;
import api.simplified.hypixel.response.skyblock.member.AccessoryBag;
import api.simplified.hypixel.response.skyblock.member.Bestiary;
import api.simplified.hypixel.response.skyblock.member.Currencies;
import api.simplified.hypixel.response.skyblock.member.GardenCore;
import api.simplified.hypixel.response.skyblock.member.JacobsContest;
import api.simplified.hypixel.response.skyblock.member.Loadouts;
import api.simplified.hypixel.response.skyblock.member.Objective;
import api.simplified.hypixel.response.skyblock.member.SkillTree;
import api.simplified.hypixel.response.skyblock.member.Statistics;
import api.simplified.hypixel.response.skyblock.member.Toolkit;
import api.simplified.hypixel.response.skyblock.member.WinterIsland;
import api.simplified.hypixel.response.skyblock.member.attribute.AttributeShards;
import api.simplified.hypixel.response.skyblock.member.crimson.CrimsonIsle;
import api.simplified.hypixel.response.skyblock.member.crimson.Dojo;
import api.simplified.hypixel.response.skyblock.member.crimson.Kuudra;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonClass;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonData;
import api.simplified.hypixel.response.skyblock.member.dungeon.DungeonRun;
import api.simplified.hypixel.response.skyblock.member.dungeon.Dungeons;
import api.simplified.hypixel.response.skyblock.member.dungeon.Floor;
import api.simplified.hypixel.response.skyblock.member.dungeon.FloorData;
import api.simplified.hypixel.response.skyblock.member.foraging.Foraging;
import api.simplified.hypixel.response.skyblock.member.foraging.HeartOfTheForest;
import api.simplified.hypixel.response.skyblock.member.hoppity.ChocolateFactory;
import api.simplified.hypixel.response.skyblock.member.mining.HeartOfTheMountain;
import api.simplified.hypixel.response.skyblock.member.pet.OwnedPet;
import api.simplified.hypixel.response.skyblock.member.rift.Rift;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.GsonSettings;
import dev.simplified.gson.annotation.Capture;
import dev.simplified.gson.annotation.Extract;
import dev.simplified.gson.annotation.Fallback;
import dev.simplified.gson.annotation.SerializedPath;
import dev.simplified.persistence.exception.JpaException;
import dev.simplified.util.Range;
import lib.minecraft.text.ChatFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the member DTO field mappings against the bundled API response.
 * <p>
 * Most tests decode a subtree because that is the smallest thing that carries the mapping under
 * test. A whole {@link SkyBlockMember} decodes here too: binding no longer runs any derivation, so
 * the SkyBlock model repositories are reached only by the accessors that need them, and only when
 * something asks.
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

    private static Set<String> lowercased(Set<String> keys) {
        return keys.stream().map(key -> key.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
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
        // getTiers() is suppressed, so the tier map is reachable only through getSpent - which is
        // what makes the internal shape free to change without a caller seeing it
        assertThat(forest.getBiomeWhispers().get("forest").getSpent(1), is(not(equalTo(0))));
        assertThat(forest.getBiomeWhispers().get("forest").getSpent(999), is(equalTo(0)));
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
            is(equalTo(Objective.Status.ACTIVE)));
        assertThat(crimsonIsle.getQuests().getQuestBoard().getMiniBossQuest().getStatus(),
            is(equalTo(Objective.Status.ACTIVE)));
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
    @DisplayName("quest rewards split into a counted item map and a quest-to-item map")
    void mapsQuestRewards() {
        JsonObject raw = rawPristine("nether_island_player_data")
            .getAsJsonObject("quests")
            .getAsJsonObject("quest_rewards");
        CrimsonIsle crimsonIsle = decodePristine("nether_island_player_data", CrimsonIsle.class);

        long expectedCounts = raw.entrySet().stream().filter(e -> e.getValue().getAsJsonPrimitive().isNumber()).count();
        long expectedItems = raw.entrySet().stream().filter(e -> e.getValue().getAsJsonPrimitive().isString()).count();

        assertThat(expectedCounts, is(not(equalTo(0L))));
        assertThat(expectedItems, is(not(equalTo(0L))));

        // both halves are typed and neither is lost
        assertThat((long) crimsonIsle.getQuests().getQuestRewards().size(), is(equalTo(expectedCounts)));
        assertThat((long) crimsonIsle.getQuests().getQuestItems().size(), is(equalTo(expectedItems)));
        assertThat(crimsonIsle.getQuests().getQuestRewards(), hasKey("KADA_LEAD"));
        assertThat(crimsonIsle.getQuests().getQuestItems(), hasKey("crimson_isle_kill_barbarian_duke_x_c"));
        // selection without stripping - the quest id IS the key
        assertThat(crimsonIsle.getQuests().getQuestItems().get("crimson_isle_kill_barbarian_duke_x_c"),
            is(equalTo("KADA_LEAD")));

        JsonObject out = JsonParser.parseString(gson.toJson(crimsonIsle)).getAsJsonObject();
        JsonObject outRewards = out.getAsJsonObject("quests").getAsJsonObject("quest_rewards");

        assertThat(outRewards.keySet(), is(equalTo(raw.keySet())));
        assertThat(out.getAsJsonObject("quests").has("questItems"), is(false));
    }

    @Test
    @DisplayName("unmatched kuudra tiers survive instead of collapsing onto one null key")
    void mapsKuudraUnknownTiers() {
        JsonObject raw = rawPristine("nether_island_player_data");
        CrimsonIsle crimsonIsle = decodePristine("nether_island_player_data", CrimsonIsle.class);

        JsonObject rawTiers = raw.getAsJsonObject("kuudra_completed_tiers");

        // no key may bind onto null, and no entry may be dropped
        assertThat(crimsonIsle.getKuudra().getCompletedTiers(), not(hasKey(nullValue(Kuudra.Tier.class))));
        assertThat(crimsonIsle.getKuudra().getHighestWave(), not(hasKey(nullValue(Kuudra.Tier.class))));

        int bound = crimsonIsle.getKuudra().getCompletedTiers().size()
            + crimsonIsle.getKuudra().getHighestWave().size()
            + crimsonIsle.getKuudra().getUnknownTiers().size();

        assertThat(bound, is(equalTo(rawTiers.size())));

        JsonObject out = JsonParser.parseString(gson.toJson(crimsonIsle)).getAsJsonObject();
        JsonObject outTiers = out.getAsJsonObject("kuudra_completed_tiers");

        // every key survives, but its CASE does not: an enum map key is written back as its
        // constant name, so the fixture's lowercase `none` returns as `NONE` and `highest_wave_hot`
        // as `highest_wave_HOT` - a literal filter prefix in front of a constant name. That is the
        // enum adapter reading case-insensitively and writing name(), and it predates this work
        assertThat(lowercased(outTiers.keySet()), is(equalTo(lowercased(rawTiers.keySet()))));
        assertThat(out.has("unknownTiers"), is(false));
    }

    @Test
    @DisplayName("dojo points bind to their types instead of overflowing as unmatched")
    void mapsDojoTypes() {
        JsonObject rawDojo = rawPristine("nether_island_player_data").getAsJsonObject("dojo");
        CrimsonIsle crimsonIsle = decodePristine("nether_island_player_data", CrimsonIsle.class);

        long rawPoints = rawDojo.keySet().stream().filter(key -> key.startsWith("dojo_points_")).count();

        assertThat(rawPoints, is(not(equalTo(0L))));

        // the wire spells these mob_kb, wall_jump and so on. Those names lived only in a
        // constructor component, which the enum adapter never sees, so every entry missed
        assertThat((long) crimsonIsle.getDojo().getPoints().size(), is(equalTo(rawPoints)));
        assertThat(crimsonIsle.getDojo().getPoints(), hasKey(Dojo.Type.FORCE));
        assertThat(crimsonIsle.getDojo().getUnknownPoints(), is(anEmptyMap()));
        assertThat(crimsonIsle.getDojo().getUnknownTimes(), is(anEmptyMap()));
    }

    @Test
    @DisplayName("currencies essence binds through the collapsed wrapper shape")
    void mapsCurrenciesEssence() {
        JsonObject rawEssence = rawPristine("currencies").getAsJsonObject("essence");
        Currencies currencies = decodePristine("currencies", Currencies.class);

        assertThat(rawEssence.size(), is(not(equalTo(0))));
        assertThat(currencies.getEssence().size(), is(equalTo(rawEssence.size())));
        assertThat(currencies.getEssence().get("WITHER"),
            is(equalTo(rawEssence.getAsJsonObject("WITHER").get("current").getAsInt())));
    }

    @Test
    @DisplayName("currencies essence re-wraps on write, dropping any wrapper sibling")
    void currenciesEssenceRoundTripDropsSiblings() {
        JsonObject own = rawPristine("currencies").deepCopy();
        // give one entry a sibling the collapsed type has no room for
        own.getAsJsonObject("essence").getAsJsonObject("WITHER").addProperty("total", 3000);

        Currencies currencies = gson.fromJson(own, Currencies.class);

        assertThat(currencies.getEssence().get("WITHER"), is(equalTo(1955)));

        JsonObject out = JsonParser.parseString(gson.toJson(currencies)).getAsJsonObject();
        JsonObject outWither = out.getAsJsonObject("essence").getAsJsonObject("WITHER");

        // pins the declared loss rather than asserting a byte-equality that does not hold: the
        // collapse is a projection, so the sibling is gone
        assertThat(outWither.get("current").getAsInt(), is(equalTo(1955)));
        assertThat(outWither.has("total"), is(false));
    }

    /**
     * Nothing carries {@code @Fallback} yet, and this is the test that says why.
     * <p>
     * The vocabulary it checks is the whole member subtree, not just the keys the enum in question
     * binds, so it is deliberately stricter than the rule it enforces - a constant named
     * {@code UNKNOWN} is rejected because some unrelated subtree carries a {@code unknown} key. That
     * conservatism is the point: a wrong mark is silent data corruption with no compile error and no
     * other test failure, while a wrong refusal costs a {@code null} that was already there. Every
     * sentinel-shaped constant in this module is either wire-named or shares a name with something
     * in that vocabulary, so adopting the marker means adding a new constant first.
     */
    @Test
    @DisplayName("no @Fallback constant is reachable from the wire")
    void noMarkedConstantIsWireVisible() throws Exception {
        List<Class<?>> enums = responseEnums();

        // guard the guard - a scan that silently found nothing would pass vacuously forever
        assertThat(enums.size(), is(greaterThan(20)));

        Set<String> vocabulary = fixtureVocabulary();

        for (Class<?> type : enums) {
            for (Object constant : type.getEnumConstants()) {
                Enum<?> value = (Enum<?>) constant;
                Field field = type.getField(value.name());

                if (!field.isAnnotationPresent(Fallback.class))
                    continue;

                // the rule, executable. A marked constant the wire can name means an unrecognised
                // value and a real one collapse onto it - and in a map key position the unknown
                // entry silently overwrites a correct one, which is worse than the null it replaces
                String where = type.getSimpleName() + "." + value.name();
                SerializedName serialized = field.getAnnotation(SerializedName.class);

                assertThat(where + " is marked @Fallback but carries @SerializedName",
                    serialized, is(nullValue()));
                assertThat(where + " is marked @Fallback but the fixture names it",
                    vocabulary.contains(value.name().toLowerCase(Locale.ROOT)), is(false));
            }
        }
    }

    /**
     * Every enum compiled under the response package, loaded from the build output directory.
     */
    private static List<Class<?>> responseEnums() throws Exception {
        Path root = Path.of(Currencies.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path pkg = root.resolve("api/simplified/hypixel/response");
        List<Class<?>> found = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(pkg)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                String name = root.relativize(path).toString()
                    .replace(java.io.File.separatorChar, '.')
                    .replaceAll("\\.class$", "");

                Class<?> type = Class.forName(name, false, MemberDtoMappingTest.class.getClassLoader());

                if (type.isEnum())
                    found.add(type);
            }
        }

        return found;
    }

    /**
     * Every key and every string value the fixture carries, lowercased - the vocabulary a marked
     * constant must not collide with.
     */
    private static Set<String> fixtureVocabulary() {
        Set<String> words = new HashSet<>();
        collectVocabulary(pristine, words);
        collectVocabulary(sparse, words);
        return words;
    }

    private static void collectVocabulary(JsonElement element, Set<String> words) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                words.add(entry.getKey().toLowerCase(Locale.ROOT));
                collectVocabulary(entry.getValue(), words);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray())
                collectVocabulary(child, words);
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
            words.add(element.getAsString().toLowerCase(Locale.ROOT));
    }

    @Test
    @DisplayName("attribute stacks decode from the member attributes object")
    void mapsAttributeStacks() {
        JsonObject stacks = populated.getAsJsonObject("attributes").getAsJsonObject("stacks");
        Map<?, ?> decoded = gson.fromJson(stacks, Map.class);

        assertThat(decoded.isEmpty(), is(false));
        assertThat(decoded, hasKey("magic_find"));
    }

    @Test
    @DisplayName("master mode pairs onto its normal floor instead of becoming a second dungeon")
    void mapsDungeonMasterMode() {
        JsonObject rawTypes = rawPristine("dungeons").getAsJsonObject("dungeon_types");
        Dungeons dungeons = decodePristine("dungeons", Dungeons.class);

        // the wire spells both halves lowercase, so a case-sensitive prefix filter let master_catacombs
        // through as its own dungeon under Type.UNKNOWN and left the real master floor unpaired
        assertThat(dungeons.getDungeons(), hasKey(DungeonData.Type.CATACOMBS));
        assertThat(dungeons.getDungeons(), not(hasKey(DungeonData.Type.UNKNOWN)));
        assertThat(dungeons.getDungeons().size(), is(equalTo(1)));

        DungeonData catacombs = dungeons.getDungeon(DungeonData.Type.CATACOMBS);

        assertThat(catacombs.getExperience(),
            is(equalTo(rawTypes.getAsJsonObject("catacombs").get("experience").getAsDouble())));
        assertThat(catacombs.getFloorData(false).getHighestCompletedTier(),
            is(equalTo(rawTypes.getAsJsonObject("catacombs").get("highest_tier_completed").getAsInt())));
        assertThat(catacombs.getFloorData(true).getHighestCompletedTier(),
            is(equalTo(rawTypes.getAsJsonObject("master_catacombs").get("highest_tier_completed").getAsInt())));

        // the two floors are separable rather than merely both present: master mode carried an empty
        // FloorData for every profile ever decoded, and the fixture gives it no times_played at all
        assertThat(catacombs.getFloorData(true).getCompletions().isEmpty(), is(false));
        assertThat(catacombs.getFloorData(true).getTimesPlayed(), is(anEmptyMap()));
        assertThat(catacombs.getFloorData(false).getTimesPlayed().isEmpty(), is(false));
    }

    @Test
    @DisplayName("a dungeon run participant reads its class and name off one matched display name")
    void mapsDungeonRunParticipant() {
        JsonObject participant = new JsonObject();
        participant.addProperty("display_name", String.format(
            "%1$sbCraftedFury%1$sf: %1$scBerserk%1$sf (%1$sd50%1$sf)",
            ChatFormat.SECTION_SYMBOL
        ));
        participant.addProperty("class_milestone", 3);

        JsonArray participants = new JsonArray();
        participants.add(participant);

        JsonObject raw = new JsonObject();
        raw.addProperty("dungeon_type", "catacombs");
        raw.addProperty("dungeon_tier", 7);
        raw.add("participants", participants);

        DungeonRun run = gson.fromJson(raw, DungeonRun.class);
        DungeonRun.Participant first = run.getParticipants().getFirst();

        // all three accessors read a group off a matcher nothing had matched, so every one of them
        // threw IllegalStateException and any path through a run participant was dead
        assertThat(run.getDungeonType(), is(equalTo(DungeonData.Type.CATACOMBS)));
        assertThat(first.getName(), is(equalTo("CraftedFury")));
        assertThat(first.getClassType(), is(equalTo(DungeonClass.Type.BERSERK)));
        assertThat(first.getClassLevel(), is(equalTo(50)));
        assertThat(first.getMilestone(), is(equalTo(3)));
    }

    @Test
    @DisplayName("a display name that carries no class falls back rather than throwing")
    void mapsDungeonRunParticipantWithoutFormatting() {
        JsonObject participant = new JsonObject();
        participant.addProperty("display_name", "CraftedFury");

        DungeonRun.Participant unformatted = gson.fromJson(participant, DungeonRun.Participant.class);

        assertThat(unformatted.getName(), is(equalTo("CraftedFury")));
        assertThat(unformatted.getClassType(), is(equalTo(DungeonClass.Type.UNKNOWN)));
        assertThat(unformatted.getClassLevel(), is(equalTo(0)));
    }

    @Test
    @DisplayName("board quest binds every status the wire spells and writes them back unchanged")
    void mapsBoardQuestStatuses() {
        assertThat(decodeStatus("COMPLETE"), is(equalTo(Objective.Status.COMPLETE)));
        assertThat(decodeStatus("ACTIVE"), is(equalTo(Objective.Status.ACTIVE)));
        // the enum spelled the finished state COMPLETED, which the wire names nowhere, so 790
        // objectives and every finished board quest bound onto null over a @NotNull default
        assertThat(decodeStatus("INACTIVE"), is(equalTo(Objective.Status.INACTIVE)));

        JsonObject raw = new JsonObject();
        raw.addProperty("status", "COMPLETE");
        raw.addProperty("progress", 3);

        Objective quest = gson.fromJson(raw, Objective.class);
        JsonObject out = JsonParser.parseString(gson.toJson(quest)).getAsJsonObject();

        assertThat(out.get("status").getAsString(), is(equalTo("COMPLETE")));
        assertThat(quest.getProgress(), is(equalTo(3)));
    }

    private static Objective.Status decodeStatus(String status) {
        JsonObject raw = new JsonObject();
        raw.addProperty("status", status);
        return gson.fromJson(raw, Objective.class).getStatus();
    }

    @Test
    @DisplayName("an auction binds its starting bid off the key the wire actually sends")
    void mapsAuctionStartingBid() {
        JsonObject raw = new JsonObject();
        raw.addProperty("starting_bid", 4_500_000L);
        raw.addProperty("highest_bid_amount", 9_000_000L);

        SkyBlockAuction auction = gson.fromJson(raw, SkyBlockAuction.class);

        // the annotation read starting_big, so every auction's starting bid bound to zero
        assertThat(auction.getStartingBid(), is(equalTo(4_500_000L)));
        assertThat(auction.getHighestBid(), is(equalTo(9_000_000L)));
    }

    @Test
    @DisplayName("the hoppity shop, time tower and hitman expose their bound fields")
    void mapsHoppitySubObjects() {
        JsonObject easter = rawPristine("events").getAsJsonObject("easter");
        JsonObject rawShop = easter.getAsJsonObject("shop");
        JsonObject rawTower = easter.getAsJsonObject("time_tower");
        JsonObject rawHitman = easter.getAsJsonObject("rabbit_hitmen");
        ChocolateFactory factory = gson.fromJson(easter.deepCopy(), ChocolateFactory.class);

        // all three classes carried no @Getter at all, so thirteen bound fields had no reader
        assertThat(factory.getShop().getYear(), is(equalTo(rawShop.get("year").getAsInt())));
        assertThat(factory.getShop().getChocolateSpent(), is(equalTo(rawShop.get("chocolate_spent").getAsLong())));
        assertThat(factory.getShop().getChocolateFortune(),
            is(equalTo(rawShop.get("cocoa_fortune_upgrades").getAsInt())));
        assertThat(factory.getShop().getRabbitsPurchased().isEmpty(), is(false));
        assertThat(factory.getTimeTower().getLevel(), is(equalTo(rawTower.get("level").getAsInt())));
        assertThat(factory.getTimeTower().getCharges(), is(equalTo(rawTower.get("charges").getAsInt())));
        assertThat(factory.getHitman().getSlots(),
            is(equalTo(rawHitman.get("rabbit_hitmen_slots").getAsInt())));
        assertThat(factory.getHitman().getEggSlotCooldownSum(),
            is(equalTo(rawHitman.get("egg_slot_cooldown_sum").getAsInt())));
    }

    @Test
    @DisplayName("jacobs contest keeps its derived contest fields off the wire")
    void jacobsContestDerivedFieldsAreNotSerialized() {
        JacobsContest jacobsContest = decodePristine("jacobs_contest", JacobsContest.class);
        JsonObject out = JsonParser.parseString(gson.toJson(jacobsContest)).getAsJsonObject();
        JsonObject outContests = out.getAsJsonObject("contests");

        assertThat(outContests.size(), is(not(equalTo(0))));
        assertThat(outContests.keySet(),
            is(equalTo(rawPristine("jacobs_contest").getAsJsonObject("contests").keySet())));

        // both are written by the enclosing hook out of the map KEY, so serializing them emitted two
        // keys per contest that the upstream shape has never carried
        for (Map.Entry<String, JsonElement> entry : outContests.entrySet()) {
            assertThat(entry.getValue().getAsJsonObject().has("skyBlockDate"), is(false));
            assertThat(entry.getValue().getAsJsonObject().has("collectionName"), is(false));
        }
    }

    /**
     * Executes the re-nesting the collapsed holders depend on.
     * <p>
     * Three fields share the {@code profile} prefix, and the write path builds that prefix once per
     * field - the first creates the object and the other two have to find and reuse it rather than
     * overwrite it. That branch was read closely and never run, so this is a prerequisite of the
     * collapse rather than a nicety.
     */
    @Test
    @DisplayName("three fields sharing one path prefix re-nest into a single object on write")
    void roundTripsSharedPathPrefix() {
        JsonObject rawProfile = rawPristine("profile");
        SharedPrefix decoded = gson.fromJson(shallowMember("profile", rawProfile), SharedPrefix.class);

        assertThat(decoded.firstJoin, is(equalTo(rawProfile.get("first_join").getAsLong())));
        assertThat(decoded.personalBankUpgrade,
            is(equalTo(rawProfile.get("personal_bank_upgrade").getAsInt())));
        assertThat(decoded.boosterCookieActive,
            is(equalTo(rawProfile.get("cookie_buff_active").getAsBoolean())));

        JsonObject out = JsonParser.parseString(gson.toJson(decoded)).getAsJsonObject();

        // the first field creates `profile` and the other two have to find and reuse it. Three
        // objects here, or two, would mean the last write won and the earlier ones were lost
        assertThat(out.keySet(), is(equalTo(Set.of("profile"))));
        assertThat(out.getAsJsonObject("profile"), is(equalTo(rawProfile)));
    }

    private static JsonObject shallowMember(String key, JsonObject value) {
        JsonObject raw = new JsonObject();
        raw.add(key, value.deepCopy());
        return raw;
    }

    /**
     * Three fields sharing one {@code @SerializedPath} prefix, declared here rather than reused from
     * the member.
     * <p>
     * The only three-field prefix that ships is on {@link SkyBlockMember}, and no whole member can be
     * serialized: {@code PlayerData.lastDeath} and several other date fields are null on a default
     * sub-object, and the {@code SkyBlockDate} adapters are not null-safe, so the write throws before
     * it reaches any path. This carries the same three paths against the same factory.
     */
    private static class SharedPrefix {

        @SerializedPath("profile.first_join")
        private long firstJoin;
        @SerializedPath("profile.personal_bank_upgrade")
        private int personalBankUpgrade;
        @SerializedPath("profile.cookie_buff_active")
        private boolean boosterCookieActive;

    }

    @Test
    @DisplayName("every relocated holder field still reads what its forwarder returned")
    void mapsRelocatedHolderFields() {
        JsonObject rawRift = rawPristine("rift");
        JsonObject rawPlaza = rawRift.getAsJsonObject("village_plaza");
        JsonObject rawProfile = rawPristine("profile");
        SkyBlockMember member = gson.fromJson(pristine.deepCopy(), SkyBlockMember.class);

        assertThat(member.getFirstJoin().getRealTime(),
            is(equalTo(rawProfile.get("first_join").getAsLong())));
        assertThat(member.getPersonalBankUpgrade(),
            is(equalTo(rawProfile.get("personal_bank_upgrade").getAsInt())));
        assertThat(member.isBoosterCookieActive(),
            is(equalTo(rawProfile.get("cookie_buff_active").getAsBoolean())));
        assertThat(member.getUnlockedTemples(), is(equalTo(List.of("forest", "desert"))));
        assertThat(member.getChocolateFactory().getChocolateLevel(),
            is(equalTo(rawPristine("events").getAsJsonObject("easter").get("chocolate_level").getAsInt())));

        Rift rift = decodePristine("rift", Rift.class);

        assertThat(rift.getKilledEyes().size(),
            is(equalTo(rawRift.getAsJsonObject("wither_cage").getAsJsonArray("killed_eyes").size())));
        assertThat(rift.getKilledEyes(), hasItem("mountaintop"));
        assertThat(rift.getVillagePlaza().getSecondsSitting(),
            is(equalTo(rawPlaza.getAsJsonObject("lonely").get("seconds_sitting").getAsInt())));
        assertThat(rift.getVillagePlaza().getSeraphineStepIndex(),
            is(equalTo(rawPlaza.getAsJsonObject("seraphine").get("step_index").getAsInt())));
        // the sibling that kept its own class must not be disturbed by the two that lost theirs
        assertThat(rift.getVillagePlaza().getMurder().getStepIndex(),
            is(equalTo(rawPlaza.getAsJsonObject("murder").get("step_index").getAsInt())));

        Dungeons dungeons = decodePristine("dungeons", Dungeons.class);

        assertThat(dungeons.getRuns(), is(empty()));
        assertThat(dungeons.getChests(), is(empty()));

        // a second, independently serializable witness for the prefix reuse: the two relocated
        // treasure fields must come back under one `treasures` object, not two
        JsonObject out = JsonParser.parseString(gson.toJson(dungeons)).getAsJsonObject();

        assertThat(out.getAsJsonObject("treasures").keySet(), is(equalTo(Set.of("runs", "chests"))));
        assertThat(out.has("runs"), is(false));
        assertThat(out.has("chests"), is(false));
    }

    @Test
    @DisplayName("the two holders with no forwarder become readable for the first time")
    void exposesPreviouslyUnreachableHolderFields() {
        JsonObject rawBestiary = rawPristine("bestiary").getAsJsonObject("miscellaneous");
        Bestiary bestiary = decodePristine("bestiary", Bestiary.class);
        AttributeShards shards = decodePristine("shards", AttributeShards.class);

        // both holders kept the class-level @Getter, so Lombok emitted an accessor returning a
        // private nested type - uncallable from anywhere outside the declaring class
        assertThat(bestiary.isMaxKillsVisible(),
            is(equalTo(rawBestiary.get("max_kills_visible").getAsBoolean())));
        assertThat(bestiary.hasNotificationsEnabled(),
            is(equalTo(rawBestiary.get("milestones_notifications").getAsBoolean())));

        // shards.traps is {} in the fixture, so the path is skipped and the default stands - the
        // point is that an eleven-field ActiveTrap list now has a reader at all
        assertThat(shards.getActiveTraps(), is(empty()));
        assertThat(shards.getOwnedShards().isEmpty(), is(false));
    }

    @Test
    @DisplayName("every objective binds, and tutorial still claims its key ahead of the catch-all")
    void capturesEveryObjective() {
        JsonObject rawObjectives = rawPristine("objectives");
        SkyBlockMember member = gson.fromJson(pristine.deepCopy(), SkyBlockMember.class);

        // one entry of the node is the tutorial list; every other one is an objective
        long expected = rawObjectives.entrySet().stream()
            .filter(entry -> entry.getValue().isJsonObject())
            .count();

        assertThat(expected, is(equalTo(792L)));
        assertThat((long) member.getObjectives().size(), is(equalTo(expected)));

        // the declared @SerializedPath claims objectives.tutorial before the catch-all descends, so
        // the list is not swallowed by a map it cannot type - this is the precedence, executed
        assertThat(member.getTutorialObjectives().size(),
            is(equalTo(rawObjectives.getAsJsonArray("tutorial").size())));
        assertThat(member.getObjectives(), not(hasKey("tutorial")));

        assertThat(member.getObjectives().get("collect_lapis").getStatus(),
            is(equalTo(Objective.Status.COMPLETE)));
        assertThat(member.getObjectives().get("talk_to_crypt_keeper_1").getStatus(),
            is(equalTo(Objective.Status.ACTIVE)));
        assertThat(member.getObjectives().get("select_mage_faction").getStatus(),
            is(equalTo(Objective.Status.INACTIVE)));
        assertThat(member.getObjectives().get("defeat_the_monster").getCompletions().orElseThrow(),
            is(equalTo(-1)));

        // an objective's own progress keys land on the value class rather than being dropped
        assertThat(member.getObjectives().get("faction_villagers").getRequirements().size(),
            is(equalTo(10)));
        assertThat(member.getObjectives().get("faction_villagers").getRequirements().get("crafter0"),
            is(equalTo(2)));
        // a boolean requirement is not an Integer, so it reaches overflow instead of binding
        assertThat(member.getObjectives().get("collect_lapis").getRequirements(), is(anEmptyMap()));
    }

    @Test
    @DisplayName("the objectives node round-trips with every key it arrived with")
    void roundTripsObjectives() {
        JsonObject rawObjectives = rawPristine("objectives");
        ObjectiveNode node = gson.fromJson(shallowMember("objectives", rawObjectives), ObjectiveNode.class);

        JsonObject out = JsonParser.parseString(gson.toJson(node)).getAsJsonObject();
        JsonObject outObjectives = out.getAsJsonObject("objectives");

        assertThat(out.keySet(), is(equalTo(Set.of("objectives"))));
        assertThat(outObjectives.keySet(), is(equalTo(rawObjectives.keySet())));

        // the six item-bearing objectives carry keys no field declares, and a boolean one is the
        // case that had to travel through overflow to get back
        assertThat(outObjectives.getAsJsonObject("collect_lapis"),
            is(equalTo(rawObjectives.getAsJsonObject("collect_lapis"))));
        assertThat(outObjectives.getAsJsonObject("faction_villagers"),
            is(equalTo(rawObjectives.getAsJsonObject("faction_villagers"))));
        assertThat(outObjectives.getAsJsonArray("tutorial"),
            is(equalTo(rawObjectives.getAsJsonArray("tutorial"))));
    }

    /**
     * The two fields that share the {@code objectives} node, declared here rather than reused from
     * the member.
     * <p>
     * No whole {@link SkyBlockMember} can be serialized - several date fields are null on a default
     * sub-object and the {@code SkyBlockDate} adapters are not null-safe - so the write half of the
     * round trip needs a vehicle carrying only the two annotations under test.
     */
    private static class ObjectiveNode {

        @SerializedName("objectives")
        @Capture(grouping = Capture.Grouping.ENTRY, descend = true)
        private ConcurrentMap<String, Objective> objectives = Concurrent.newMap();
        @Extract("objectives.tutorial")
        private ConcurrentList<String> tutorialObjectives = Concurrent.newList();

    }

    @Test
    @DisplayName("election cycles compute the same bounds the hook used to store")
    void computesElectionCycles() {
        Election election = new Election(278);

        // captured off the hook before it was removed, so these are the pre-change values rather
        // than a restatement of the expressions that now produce them
        assertThat(election.getVoting().getStart().getRealTime(), is(equalTo(1684145700000L)));
        assertThat(election.getVoting().getEnd().getRealTime(), is(equalTo(1684480500000L)));
        assertThat(election.getTerm().getStart().getRealTime(), is(equalTo(1684480500000L)));
        assertThat(election.getTerm().getEnd().getRealTime(), is(equalTo(1684926900000L)));

        // a term begins the moment voting closes
        assertThat(election.getTerm().getStart().getRealTime(),
            is(equalTo(election.getVoting().getEnd().getRealTime())));

        // the no-arg constructor no longer leaves a half-built object: gson binds year, and both
        // cycles follow from it whenever they are asked for
        assertThat(new Election().getVoting().getStart().getRealTime(),
            is(not(equalTo(election.getVoting().getStart().getRealTime()))));
    }

    @Test
    @DisplayName("two elections of one year are equal and hash alike")
    void electionIdentityIsItsYear() {
        // Cycle declares no equals, so folding the derived cycles into identity made this false
        assertThat(new Election(278), is(equalTo(new Election(278))));
        assertThat(new Election(278).hashCode(), is(equalTo(new Election(278).hashCode())));
        assertThat(new Election(278), is(not(equalTo(new Election(279)))));
    }

    @Test
    @DisplayName("the party finder binds as kuudra's sibling and round-trips as one")
    void mapsKuudraPartyFinder() {
        JsonObject rawFinder = rawPristine("nether_island_player_data")
            .getAsJsonObject("kuudra_party_finder");
        CrimsonIsle crimsonIsle = decodePristine("nether_island_player_data", CrimsonIsle.class);

        assertThat(crimsonIsle.getPartyFinderSearch().getTier(), is(equalTo(Kuudra.Tier.INFERNAL)));
        assertThat(crimsonIsle.getPartyFinderSearch().getSort(),
            is(equalTo(Kuudra.SearchSettings.Sort.HIGHEST_COMBAT_LEVEL)));
        assertThat(crimsonIsle.getPartyFinderSearch().getCombatLevel().getMinimum(), is(equalTo(5)));
        assertThat(crimsonIsle.getPartyFinderGroupBuilder().getNote().orElseThrow(), is(equalTo("spec")));
        assertThat(crimsonIsle.getPartyFinderGroupBuilder().getRequiredCombatLevel(),
            is(equalTo(rawFinder.getAsJsonObject("group_builder").get("combat_level_required").getAsInt())));

        JsonObject out = JsonParser.parseString(gson.toJson(crimsonIsle)).getAsJsonObject();
        JsonObject outFinder = out.getAsJsonObject("kuudra_party_finder");

        // a new capability rather than a preserved one: the values used to be copied onto two
        // transient fields of Kuudra, which were never serialized at all
        assertThat(outFinder.keySet(), is(equalTo(rawFinder.keySet())));
        assertThat(outFinder.getAsJsonObject("group_builder").get("combat_level_required").getAsInt(),
            is(equalTo(rawFinder.getAsJsonObject("group_builder").get("combat_level_required").getAsInt())));
        // the range is split into a pair on read and re-joined on write, so the wire string returns
        assertThat(outFinder.getAsJsonObject("search_settings").get("combat_level").getAsString(),
            is(equalTo(rawFinder.getAsJsonObject("search_settings").get("combat_level").getAsString())));
        assertThat(out.has("partyFinderSearch"), is(false));
        assertThat(out.has("partyFinderGroupBuilder"), is(false));
    }

    @Test
    @DisplayName("a combat range the wire never sends falls back instead of throwing")
    void kuudraCombatRangeFallsBack() {
        // the hand-rolled parse split on '-' and called Integer.parseInt on both halves, so an
        // absent or malformed range threw at the caller rather than yielding the default
        JsonObject empty = new JsonObject();

        assertThat(gson.fromJson(empty, Kuudra.SearchSettings.class).getCombatLevel(),
            is(equalTo(Range.between(0, 60))));

        JsonObject malformed = new JsonObject();
        malformed.addProperty("combat_level", "anything");

        assertThat(gson.fromJson(malformed, Kuudra.SearchSettings.class).getCombatLevel(),
            is(equalTo(Range.between(0, 60))));
    }

    @Test
    @DisplayName("rift statistics type as counters and give the vermin tally its own class")
    void mapsRiftStatistics() {
        JsonObject rawRift = rawPristine("player_stats").getAsJsonObject("rift");
        Statistics statistics = decodePristine("player_stats", Statistics.class);

        long counters = rawRift.entrySet().stream()
            .filter(entry -> entry.getValue().isJsonPrimitive())
            .count();

        // the node was one Object map; the counters are now typed and the two objects are not
        assertThat(counters, is(equalTo(29L)));
        assertThat((long) statistics.getRiftStats().size(), is(equalTo(counters)));
        assertThat(statistics.getRiftStats().get("visits"),
            is(equalTo(rawRift.get("visits").getAsInt())));
        assertThat(statistics.getRiftStats(), not(hasKey("west_vermin_vacuumed")));

        JsonObject rawVermin = rawRift.getAsJsonObject("west_vermin_vacuumed");

        assertThat(statistics.getVerminVacuumed().getTotal(),
            is(equalTo(rawVermin.get("total").getAsInt())));
        assertThat(statistics.getVerminVacuumed().getSilverfish(),
            is(equalTo(rawVermin.get("silverfish").getAsInt())));
        assertThat(statistics.getVerminVacuumed().getMosquito(),
            is(equalTo(rawVermin.get("mosquito").getAsInt())));

        JsonObject out = JsonParser.parseString(gson.toJson(statistics)).getAsJsonObject();
        JsonObject outRift = out.getAsJsonObject("rift");

        // the claimed object goes back inside rift, and the one left in overflow comes with it
        assertThat(outRift.keySet(), is(equalTo(rawRift.keySet())));
        assertThat(outRift.getAsJsonObject("west_vermin_vacuumed"), is(equalTo(rawVermin)));
        assertThat(outRift.getAsJsonObject("shen_item_bought"),
            is(equalTo(rawRift.getAsJsonObject("shen_item_bought"))));
        assertThat(out.has("verminVacuumed"), is(false));
    }

    @Test
    @DisplayName("pet extra counters type as numbers instead of Object")
    void mapsPetExtra() {
        boolean anyExtra = false;

        for (JsonElement element : pristine.getAsJsonObject("pets_data").getAsJsonArray("pets")) {
            OwnedPet pet = gson.fromJson(element.deepCopy(), OwnedPet.class);

            for (Map.Entry<String, Long> entry : pet.getExtra().entrySet()) {
                anyExtra = true;
                assertThat(entry.getValue(), is(notNullValue()));
            }
        }

        assertThat(anyExtra, is(true));
    }

    @Test
    @DisplayName("class damage captures onto its class and re-prefixes on write")
    void mapsFloorMostDamage() {
        JsonObject rawCatacombs = rawPristine("dungeons")
            .getAsJsonObject("dungeon_types")
            .getAsJsonObject("catacombs");
        Dungeons dungeons = decodePristine("dungeons", Dungeons.class);
        FloorData normal = dungeons.getDungeon(DungeonData.Type.CATACOMBS).getFloorData(false);

        // five near-identical fields and a switch over them became one map keyed by the class
        assertThat(normal.getMostDamage().size(), is(equalTo(5)));
        assertThat(normal.getMostDamage(DungeonClass.Type.HEALER).get(Floor.SEVEN),
            is(equalTo(rawCatacombs.getAsJsonObject("most_damage_healer").get("7").getAsDouble())));
        assertThat(normal.getMostDamage(DungeonClass.Type.ARCHER).size(),
            is(equalTo(rawCatacombs.getAsJsonObject("most_damage_archer").size())));
        assertThat(normal.getMostDamage(DungeonClass.Type.UNKNOWN), is(anEmptyMap()));

        // the two neighbours that could have been captured by accident were not
        assertThat(normal.getMostHealing().isEmpty(), is(false));
        assertThat(normal.getMostMobsKilled().isEmpty(), is(false));

        JsonObject outCatacombs = JsonParser.parseString(gson.toJson(dungeons))
            .getAsJsonObject()
            .getAsJsonObject("dungeon_types")
            .getAsJsonObject("catacombs");

        // the filter's literal prefix goes back in front of each key, so no most_damage_ entry is
        // lost - but an enum key is written as its constant name, so the wire's lowercase spelling
        // returns uppercased. Same case drift the kuudra tiers already document
        assertThat(lowercased(outCatacombs.keySet()), is(equalTo(lowercased(rawCatacombs.keySet()))));
        assertThat(outCatacombs.has("mostDamage"), is(false));
    }

    @Test
    @DisplayName("a player's skyblock profiles bind by path and re-nest on write")
    void mapsHypixelPlayerProfiles() {
        JsonObject first = new JsonObject();
        first.addProperty("profile_id", "d3b1a2c4-0000-4000-8000-000000000001");
        first.addProperty("cute_name", "Mango");

        JsonObject second = new JsonObject();
        second.addProperty("profile_id", "d3b1a2c4-0000-4000-8000-000000000002");
        second.addProperty("cute_name", "Papaya");

        JsonObject profiles = new JsonObject();
        profiles.add("d3b1a2c40000400080000000000000 01".replace(" ", ""), first);
        profiles.add("d3b1a2c40000400080000000000000 02".replace(" ", ""), second);

        JsonObject skyBlock = new JsonObject();
        skyBlock.add("profiles", profiles);

        JsonObject stats = new JsonObject();
        stats.add("SkyBlock", skyBlock);

        JsonArray achievements = new JsonArray();
        achievements.add("general_wealth");
        achievements.add(7);
        achievements.add("skyblock_minion_lover");

        JsonObject raw = new JsonObject();
        raw.add("stats", stats);
        raw.add("achievementsOneTime", achievements);

        HypixelPlayer player = gson.fromJson(raw, HypixelPlayer.class);

        // two nested classes existed only to name stats and SkyBlock on the way to profiles
        assertThat(player.getSkyBlockProfiles().size(), is(equalTo(2)));
        assertThat(player.getSkyBlockProfiles().getFirst().getProfileName(), is(equalTo("Mango")));
        assertThat(player.getSkyBlockProfiles().getLast().getProfileName(), is(equalTo("Papaya")));

        // the numeric entry is not an achievement name, so it filters into overflow
        assertThat(player.getAchievementsOneTime(),
            is(equalTo(List.of("general_wealth", "skyblock_minion_lover"))));

        JsonObject out = JsonParser.parseString(gson.toJson(player)).getAsJsonObject();

        assertThat(out.getAsJsonObject("stats").getAsJsonObject("SkyBlock").getAsJsonObject("profiles").keySet(),
            is(equalTo(profiles.keySet())));
        assertThat(out.has("skyBlockProfiles"), is(false));

        // nothing is lost, but position is not preserved: a partially overflowed array puts its
        // filtered entries back at the end rather than at the index the wire gave them. Membership
        // round-trips, order does not, and the old hand-written filter dropped the entry outright
        JsonArray outAchievements = out.getAsJsonArray("achievementsOneTime");

        assertThat(outAchievements.size(), is(equalTo(achievements.size())));
        assertThat(outAchievements.contains(achievements.get(1)), is(true));
        assertThat(outAchievements.get(outAchievements.size() - 1), is(equalTo(achievements.get(1))));
    }

    @Test
    @DisplayName("jacobs contests collapse onto their key and parse a colon-bearing collection id")
    void mapsJacobsContests() {
        JsonObject rawContests = rawPristine("jacobs_contest").getAsJsonObject("contests");
        JacobsContest jacobsContest = decodePristine("jacobs_contest", JacobsContest.class);

        assertThat(jacobsContest.getContests().size(), is(equalTo(rawContests.size())));
        assertThat(jacobsContest.getContests().size(), is(equalTo(810)));

        JacobsContest.Contest pumpkin = contestById(jacobsContest, "99:6_27:PUMPKIN");

        assertThat(pumpkin.getCollectionName(), is(equalTo("PUMPKIN")));
        assertThat(pumpkin.getSkyBlockDate().getYear(), is(equalTo(99)));
        assertThat(pumpkin.getCollected(),
            is(equalTo(rawContests.getAsJsonObject("99:6_27:PUMPKIN").get("collected").getAsInt())));

        // the id splits into four parts, not three - truncating at three gives INK_SACK and passes
        // every assertion written against the other 758 keys
        JacobsContest.Contest brownDye = contestById(jacobsContest, "229:5_31:INK_SACK:3");

        assertThat(brownDye.getCollectionName(), is(equalTo("INK_SACK:3")));
        assertThat(brownDye.getSkyBlockDate().getYear(), is(equalTo(229)));

        JsonObject out = JsonParser.parseString(gson.toJson(jacobsContest)).getAsJsonObject();
        JsonObject outContests = out.getAsJsonObject("contests");

        assertThat(outContests.keySet(), is(equalTo(rawContests.keySet())));
        assertThat(outContests.getAsJsonObject("229:5_31:INK_SACK:3"),
            is(equalTo(rawContests.getAsJsonObject("229:5_31:INK_SACK:3"))));
    }

    private static JacobsContest.Contest contestById(JacobsContest jacobsContest, String id) {
        return jacobsContest.getContests()
            .stream()
            .filter(contest -> contest.getId().equals(id))
            .findFirst()
            .orElseThrow();
    }

    @Test
    @DisplayName("dungeon classes bind straight off player_classes")
    void mapsDungeonClasses() {
        JsonObject rawClasses = rawPristine("dungeons").getAsJsonObject("player_classes");
        Dungeons dungeons = decodePristine("dungeons", Dungeons.class);

        // DungeonClass has one field and the wire node already is that shape, so the funnel that
        // reduced each value to its experience had nothing left to do
        assertThat(dungeons.getClasses().size(), is(equalTo(rawClasses.size())));
        assertThat(dungeons.getClasses().size(), is(equalTo(5)));
        assertThat(dungeons.getClass(DungeonClass.Type.HEALER).getExperience(),
            is(equalTo(rawClasses.getAsJsonObject("healer").get("experience").getAsDouble())));
        assertThat(dungeons.getClass(DungeonClass.Type.UNKNOWN).getExperience(), is(equalTo(0.0)));

        double expectedTotal = rawClasses.entrySet().stream()
            .mapToDouble(entry -> entry.getValue().getAsJsonObject().get("experience").getAsDouble())
            .sum();

        assertThat(dungeons.getClassExperience(), is(equalTo(expectedTotal)));
        assertThat(dungeons.getClassWeight().size(), is(equalTo(5)));

        JsonObject out = JsonParser.parseString(gson.toJson(dungeons)).getAsJsonObject();

        assertThat(lowercased(out.getAsJsonObject("player_classes").keySet()),
            is(equalTo(lowercased(rawClasses.keySet()))));
    }

    /**
     * Pins where the bestiary join stops in a session-less test.
     * <p>
     * The mob parse used to throw {@link IllegalStateException} on the very first key, because the
     * matcher that ran {@code matches()} and the matcher the groups were read from were two different
     * objects - and it ran at bind time, so the throw landed in a swallowing catch and left
     * {@code families} empty for every profile ever decoded. It is now reached only on demand, so the
     * parse completes and the repository is the honest boundary for a test that stands up no session.
     */
    @Test
    @DisplayName("bestiary parses every mob key before it reaches the family repository")
    void bestiaryParsesEveryMobKey() {
        Bestiary bestiary = decodePristine("bestiary", Bestiary.class);

        // decoding no longer touches the repository at all
        assertThat(bestiary.getKills().isEmpty(), is(false));
        assertThrows(JpaException.class, bestiary::getFamilies);
    }

    @Test
    @DisplayName("a whole member decodes with no session, and wires the accessory bag on access")
    void decodesWholeMember() {
        SkyBlockMember member = gson.fromJson(pristine.deepCopy(), SkyBlockMember.class);
        String talismanBag = member.getInventory().getBags().getAccessories().getRawData();

        assertThat(talismanBag.isEmpty(), is(false));

        // initialize() ran at bind time and read `contents` eighty lines before assigning it, so it
        // parsed the empty default and threw on the first statement of every member's postInit
        assertThat(member.getAccessoryBag().getContents().getRawData(), is(equalTo(talismanBag)));

        // the parse behind it is the only part that still needs a repository, and only on demand
        assertThrows(JpaException.class, () -> member.getAccessoryBag().getDetectedAccessories());

        assertThat(member.getSkills(), is(notNullValue()));
    }

    @Test
    @DisplayName("an accessory bag decoded on its own stays empty instead of throwing")
    void mapsStandaloneAccessoryBag() {
        AccessoryBag bag = decodePristine("accessory_bag_storage", AccessoryBag.class);

        // no member ever calls initialize here, so there is no talisman bag to parse
        assertThat(bag.getContents().getRawData(), is(equalTo("")));
        assertThat(bag.getDetectedAccessories(), is(empty()));
        assertThat(bag.getAccessories(), is(empty()));
        assertThat(bag.getMagicalPower(), is(equalTo(0)));
    }

    @Test
    @DisplayName("collection tiers index in one pass, keep colons, and memoise")
    void mapsCollectionUnlocked() {
        SkyBlockMember member = gson.fromJson(pristine.deepCopy(), SkyBlockMember.class);
        ConcurrentMap<String, Integer> unlocked = member.getCollectionUnlocked();

        assertThat(unlocked.size(), is(equalTo(member.getCollection().size())));
        assertThat(unlocked.size(), is(equalTo(100)));
        assertThat(unlocked.get("WHEAT"), is(equalTo(11)));

        // the id carries a colon and an underscore of its own, so splitting anywhere but the LAST
        // underscore truncates it - INK_SACK:3 and INK_SACK are different collections
        assertThat(unlocked.get("INK_SACK:3"), is(equalTo(9)));

        // a collected item with nothing claimed is tier zero rather than absent, and the 83 entries
        // spelled _-1 are excluded rather than read as a negative maximum
        assertThat(unlocked.values().stream().filter(tier -> tier == 0).count(), is(equalTo(11L)));

        assertThat(member.getCollectionUnlocked(), is(sameInstance(unlocked)));
    }

    @Test
    @DisplayName("a dungeon and a dungeon class weigh the same way")
    void weighsDungeonProgressions() {
        Dungeons dungeons = decodePristine("dungeons", Dungeons.class);
        DungeonData catacombs = dungeons.getDungeon(DungeonData.Type.CATACOMBS);
        DungeonClass healer = dungeons.getClass(DungeonClass.Type.HEALER);

        // captured before the two byte-identical copies of the formula were folded into one.
        // SkyBlockMember.getTotalWeight() sums both, so a divergence would not have shown up there
        assertThat(catacombs.getWeight().getValue(), is(equalTo(200.0)));
        assertThat(catacombs.getWeight().getOverflow(), is(equalTo(2.03)));
        assertThat(healer.getWeight().getValue(), is(equalTo(90.6)));
        assertThat(healer.getWeight().getOverflow(), is(equalTo(0.0)));

        // a dungeon reads the experience of its normal floor, a class reads its own
        assertThat(catacombs.getExperience(), is(equalTo(catacombs.getNormalMode().getExperience())));
        assertThat(catacombs.getExperienceTiers(), is(equalTo(healer.getExperienceTiers())));
        assertThat(catacombs.getMaxLevel(), is(equalTo(50)));
    }

}
