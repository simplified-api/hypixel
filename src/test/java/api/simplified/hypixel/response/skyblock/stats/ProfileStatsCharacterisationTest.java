package api.simplified.hypixel.response.skyblock.stats;
import api.simplified.hypixel.response.skyblock.SkyBlockIsland;
import api.simplified.hypixel.response.skyblock.SkyBlockMember;
import api.simplified.hypixel.response.skyblock.member.CenturyCake;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.BonusArmorSet;
import api.simplified.skyblock.model.BonusEnchantmentStat;
import api.simplified.skyblock.model.BonusItemRarity;
import api.simplified.skyblock.model.BonusItemStat;
import api.simplified.skyblock.model.BonusPetPerkStat;
import api.simplified.skyblock.model.BonusReforgeStat;
import api.simplified.skyblock.model.Buff;
import api.simplified.skyblock.model.Stat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.simplified.collection.ConcurrentLinkedMap;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.collection.ConcurrentMap;
import dev.simplified.gson.GsonSettings;
import dev.simplified.persistence.JpaSession;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
/**
 * Pins every number one compute produces over the bundled fixture, so a refactor that moves one says
 * which one.
 * <p>
 * The comparison is against a golden file rather than against a second live compute, because two
 * computes for one member are not independent: the accessory bag memoises its its accessories
 * on the member itself, and of the two bonuses applied to those tables one latches and one does not,
 * so a second run is neither a repeat nor a clean slate. A frozen file has no such problem.
 * <p>
 * Only cells something wrote are recorded. Every other cell is seeded at zero and stays there, so
 * omitting them costs no coverage and keeps a moved number to one line of diff.
 * <p>
 * Regenerate with {@code -Dprofile-stats.golden=write}, never automatically - a golden file that
 * rewrites itself proves nothing.
 */
class ProfileStatsCharacterisationTest {
    private static final String GOLDEN_RESOURCE = "/profile-stats-golden.json";
    private static final Path GOLDEN_SOURCE = Path.of("src", "test", "resources", "profile-stats-golden.json");
    private static final String WRITE_PROPERTY = "profile-stats.golden";
    private static final String CENTURY_CAKE_CELLS = "profile/" + StatSource.CENTURY_CAKES.name() + "/";
    /**
     * Relative tolerance for a totalled double.
     * <p>
     * Totals are summed over concurrent map iteration order and double addition is not associative,
     * so reproducing a number bit for bit is not a property this design guarantees.
     */
    private static final double EPSILON = 1.0e-9;
    private static JpaSession session;
    private static ProfileStats profileStats;
    private static SkyBlockMember member;
    private static String fixtureSha256;
    private static String corpusCommitSha;
    @BeforeAll
    static void computeFixture() throws Exception {
        Optional<Path> corpus = LocalSkyBlockData.findCorpus();
        assumeTrue(corpus.isPresent(), "no skyblock-data checkout beside this module, and none named by -D" + LocalSkyBlockData.ROOT_PROPERTY);
        ConcurrentList<String> uncovered = LocalSkyBlockData.uncoveredModels(corpus.get());
        assumeTrue(uncovered.isEmpty(), "the reference models and the corpus are of different vintages - the corpus carries no file for " + uncovered);
        corpusCommitSha = LocalSkyBlockData.corpusCommitSha(corpus.get()).orElse("");
        session = LocalSkyBlockData.connect(corpus.get());
        byte[] fixture;
        try (InputStream stream = ProfileStatsCharacterisationTest.class.getResourceAsStream("/craftedfury.json")) {
            if (stream == null)
                throw new IllegalStateException("craftedfury.json is missing from the classpath");
            fixture = stream.readAllBytes();
        }
        fixtureSha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fixture));
        Gson gson = GsonSettings.defaults().create();
        JsonObject root = JsonParser.parseReader(new InputStreamReader(new ByteArrayInputStream(fixture), StandardCharsets.UTF_8)).getAsJsonObject();
        SkyBlockIsland island = gson.fromJson(root.getAsJsonArray("profiles").get(1), SkyBlockIsland.class);
        member = island.getMembers().stream().values().findFirst().orElseThrow();
        profileStats = ProfileStats.compute(island, member);
    }
    @AfterAll
    static void releaseSession() {
        LocalSkyBlockData.disconnect(session);
        session = null;
    }
    @Test
    @DisplayName("every written cell matches the golden file")
    void everyWrittenCellMatchesTheGoldenFile() throws Exception {
        Map<String, Double> actual = collect();
        if ("write".equals(System.getProperty(WRITE_PROPERTY))) {
            write(actual);
            return;
        }
        JsonObject golden = readGolden();
        JsonObject header = golden.getAsJsonObject("header");
        assertThat(
            "the fixture has changed, so the golden file describes a different capture - regenerate with -D" + WRITE_PROPERTY + "=write",
            header.get("fixtureSha256").getAsString(),
            is(equalTo(fixtureSha256))
        );
        assertThat(
            "the corpus has moved under the golden file - regenerate with -D" + WRITE_PROPERTY + "=write",
            header.get("corpusCommitSha").getAsString(),
            is(equalTo(corpusCommitSha))
        );
        // a cake is evidence only while it is in date, and the entries of a frozen capture never leave, so an
        // expired bag has nothing to say about its eighteen cells rather than something wrong to say
        int activeCakes = activeCenturyCakes();
        if (activeCakes > 0)
            assertThat(
                "century cakes have expired since the golden file was taken, so eighteen stats read lower - regenerate with -D" + WRITE_PROPERTY + "=write",
                header.get("activeCenturyCakes").getAsInt(),
                is(equalTo(activeCakes))
            );
        JsonObject expected = golden.getAsJsonObject("cells");
        StringBuilder drift = new StringBuilder();
        expected.keySet()
            .stream()
            .filter(path -> activeCakes > 0 || !path.startsWith(CENTURY_CAKE_CELLS))
            .filter(path -> !actual.containsKey(path))
            .forEach(path -> drift.append(String.format("%n  %s: %s became nothing", path, expected.get(path).getAsDouble())));
        actual.forEach((path, value) -> {
            if (!expected.has(path)) {
                drift.append(String.format("%n  %s: nothing became %s", path, value));
                return;
            }
            double frozen = expected.get(path).getAsDouble();
            if (Math.abs(frozen - value) > EPSILON * Math.max(1.0, Math.abs(frozen)))
                drift.append(String.format("%n  %s: %s became %s", path, frozen, value));
        });
        assertThat("computed numbers moved:" + drift, drift.length(), is(equalTo(0)));
    }
    @Test
    @DisplayName("the golden file covers the sources the fixture drives")
    void theGoldenFileCoversTheSourcesTheFixtureDrives() {
        Map<String, Double> actual = collect();
        assertThat(actual.size(), is(greaterThan(200)));
        assertThat(actual, hasKey("damageMultiplier"));
        // one per source the fixture is known to feed, so a source that quietly stops contributing fails here
        for (String source : new String[] { "BASE_STATS", "SKILLS", "SLAYERS", "DUNGEONS", "SKYBLOCK_LEVELS", "BESTIARY", "PET_SCORE", "MELODYS_HARP", "JACOBS_FARMING", "BOOSTER_COOKIE", "ESSENCE", "ACCESSORY_POWER", "ACTIVE_PET" })
            assertThat(source + " contributes nothing", actual.keySet().stream().anyMatch(path -> path.startsWith("profile/" + source + "/")), is(true));
        // century cakes are the one source that runs out, so they are held to the same check only while one is in date
        if (activeCenturyCakes() > 0)
            assertThat("CENTURY_CAKES contributes nothing", actual.keySet().stream().anyMatch(path -> path.startsWith(CENTURY_CAKE_CELLS)), is(true));
    }
    @Test
    @DisplayName("the store holds only the cells something wrote")
    void theStoreHoldsOnlyTheCellsSomethingWrote() {
        int knownStats = profileStats.getAllStats().size();
        int written = writtenCells(profileStats.getStats());
        int seeded = StatSource.values().length * knownStats;
        for (StatSheet.Slot slot : profileStats.getSheet().getSlots().values()) {
            written += writtenCells(slot.table().getEntries());
            seeded += ItemOrigin.values().length * knownStats;
        }
        // the dense shape this replaces gave every bucket a value for every stat before a source ran
        assertThat(seeded, is(greaterThan(40_000)));
        assertThat(String.format("%d cells written against %d seeded", written, seeded), written, is(lessThan(seeded / 20)));
    }
    @Test
    @DisplayName("no row gates on an hour, so no total depends on the time it is read")
    void noRowGatesOnAnHourSoNoTotalDependsOnTheTimeItIsRead() {
        // the buff table has rows now, so the property is checked rather than implied by emptiness -
        // an hour-valued gate is what would make a total depend on when it was computed
        ConcurrentList<Buff> buffs = SkyBlockData.getRepository(Buff.class).findAll();
        assertThat("the rarity rows are missing", buffs.size(), is(equalTo(6)));
        for (Buff row : buffs)
            assertThat(row.getId() + " gates on an hour", gatesOnAnHour(row), is(false));
        // the six legacy tables can still carry one until they retire, and they are still empty
        assertThat(SkyBlockData.getRepository(BonusItemStat.class).findAll(), is(empty()));
        assertThat(SkyBlockData.getRepository(BonusReforgeStat.class).findAll(), is(empty()));
        assertThat(SkyBlockData.getRepository(BonusArmorSet.class).findAll(), is(empty()));
        assertThat(SkyBlockData.getRepository(BonusPetPerkStat.class).findAll(), is(empty()));
        assertThat(SkyBlockData.getRepository(BonusEnchantmentStat.class).findAll(), is(empty()));
        assertThat(SkyBlockData.getRepository(BonusItemRarity.class).findAll(), is(empty()));
    }
    private static boolean gatesOnAnHour(Buff row) {
        return Stream.concat(row.getConditions().stream(), row.getRules().stream().flatMap(rule -> rule.getWhen().stream()))
            .anyMatch(ProfileStatsCharacterisationTest::readsAnHour);
    }
    private static boolean readsAnHour(Buff.Condition condition) {
        if (condition.getInput() != null && condition.getInput().getWorld() == Buff.Term.World.HOUR)
            return true;
        if (condition.getNot() != null && readsAnHour(condition.getNot()))
            return true;
        return Stream.concat(condition.getAll().stream(), condition.getAny().stream())
            .anyMatch(ProfileStatsCharacterisationTest::readsAnHour);
    }
    private static int activeCenturyCakes() {
        return (int) member.getPlayerData()
            .getCenturyCakes()
            .stream()
            .filter(CenturyCake::isActive)
            .count();
    }
    private static Map<String, Double> collect() {
        Map<String, Double> cells = new TreeMap<>();
        record(cells, "damageMultiplier", profileStats.getDamageMultiplier());
        collectTable(cells, "profile", profileStats.getStats());
        profileStats.getSheet()
            .getSlots()
            .forEach((address, slot) -> collectTable(cells, owner(address, slot.stack()), slot.table().getEntries()));
        return cells;
    }
    /**
     * The golden file's owner key for one slot, which the sheet has to reproduce exactly - a re-keyed
     * cell reads as "nothing became" plus "became nothing" rather than as a moved number.
     */
    private static String owner(ItemSlot address, ItemStack stack) {
        return switch (address.kind()) {
            case ARMOR -> String.format("armor/%d:%s", address.index(), stack.getItem().getId());
            case ACCESSORY -> String.format("accessory/%03d:%s", address.index(), stack.getItem().getId());
        };
    }
    private static void collectTable(Map<String, Double> cells, String owner, ConcurrentMap<StatOrigin, ConcurrentLinkedMap<Stat, Data>> entries) {
        entries.forEach((bucket, statEntries) -> statEntries.forEach((statModel, data) -> {
            String path = String.format("%s/%s/%s", owner, bucket.name(), statModel.getId());
            record(cells, path + "/base", data.getBase());
            record(cells, path + "/bonus", data.getBonus());
        }));
    }
    private static int writtenCells(ConcurrentMap<StatOrigin, ConcurrentLinkedMap<Stat, Data>> entries) {
        return entries.stream()
            .mapToInt(entry -> entry.getValue().size())
            .sum();
    }
    private static void record(Map<String, Double> cells, String path, double value) {
        if (value != 0.0)
            cells.put(path, value);
    }
    private static JsonObject readGolden() throws Exception {
        try (InputStream stream = ProfileStatsCharacterisationTest.class.getResourceAsStream(GOLDEN_RESOURCE)) {
            if (stream == null)
                throw new IllegalStateException("profile-stats-golden.json is missing from the classpath - take a baseline with -D" + WRITE_PROPERTY + "=write");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    private static void write(Map<String, Double> cells) throws Exception {
        JsonObject header = new JsonObject();
        header.addProperty("fixtureSha256", fixtureSha256);
        header.addProperty("corpusCommitSha", corpusCommitSha);
        header.addProperty("activeCenturyCakes", activeCenturyCakes());
        JsonObject values = new JsonObject();
        cells.forEach(values::addProperty);
        JsonObject golden = new JsonObject();
        golden.add("header", header);
        golden.add("cells", values);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(GOLDEN_SOURCE, gson.toJson(golden).replace("\r\n", "\n"));
    }
}
