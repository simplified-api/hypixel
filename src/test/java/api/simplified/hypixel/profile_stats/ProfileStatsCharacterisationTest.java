package api.simplified.hypixel.profile_stats;

import api.simplified.hypixel.profile_stats.data.AccessoryData;
import api.simplified.hypixel.profile_stats.data.Data;
import api.simplified.hypixel.profile_stats.data.ItemData;
import api.simplified.hypixel.profile_stats.data.StatData;
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
import api.simplified.skyblock.model.Stat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.simplified.collection.ConcurrentList;
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
 * computes for one member are not independent: the accessory bag memoises its {@link AccessoryData}
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
        assertThat(
            "century cakes have expired since the golden file was taken, so eighteen stats read lower - regenerate with -D" + WRITE_PROPERTY + "=write",
            header.get("activeCenturyCakes").getAsInt(),
            is(equalTo(activeCenturyCakes()))
        );

        JsonObject expected = golden.getAsJsonObject("cells");
        StringBuilder drift = new StringBuilder();

        expected.keySet()
            .stream()
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
        for (String source : new String[] { "BASE_STATS", "SKILLS", "SLAYERS", "DUNGEONS", "SKYBLOCK_LEVELS", "BESTIARY", "PET_SCORE", "MELODYS_HARP", "JACOBS_FARMING", "BOOSTER_COOKIE", "ESSENCE", "ACCESSORY_POWER", "ACTIVE_PET", "CENTURY_CAKES" })
            assertThat(source + " contributes nothing", actual.keySet().stream().anyMatch(path -> path.startsWith("profile/" + source + "/")), is(true));
    }

    @Test
    @DisplayName("the store holds only the cells something wrote")
    void theStoreHoldsOnlyTheCellsSomethingWrote() {
        int knownStats = profileStats.getAllStats().size();

        int written = writtenCells(profileStats);
        int seeded = ProfileStats.Type.values().length * knownStats;

        for (Optional<ItemData> armorPiece : profileStats.getArmor()) {
            if (armorPiece.isPresent()) {
                written += writtenCells(armorPiece.get());
                seeded += ItemData.Type.values().length * knownStats;
            }
        }

        for (AccessoryData accessoryData : profileStats.getAccessoryBag().getAccessories()) {
            written += writtenCells(accessoryData);
            seeded += AccessoryData.Type.values().length * knownStats;
        }

        // the dense shape this replaces gave every bucket a value for every stat before a source ran
        assertThat(seeded, is(greaterThan(40_000)));
        assertThat(String.format("%d cells written against %d seeded", written, seeded), written, is(lessThan(seeded / 20)));
    }

    @Test
    @DisplayName("no buff row exists, so no total depends on the hour it is read")
    void noBuffRowExistsSoNoTotalDependsOnTheHourItIsRead() {
        // a TIME guard zeroes a running value outside its hour ranges, and it can only arrive on one of these
        assertThat(SkyBlockData.getRepository(BonusItemStat.class).findAll(), is(empty()));
        assertThat(SkyBlockData.getRepository(BonusReforgeStat.class).findAll(), is(empty()));
        assertThat(SkyBlockData.getRepository(BonusArmorSet.class).findAll(), is(empty()));
        assertThat(SkyBlockData.getRepository(BonusPetPerkStat.class).findAll(), is(empty()));
        assertThat(SkyBlockData.getRepository(BonusEnchantmentStat.class).findAll(), is(empty()));
        assertThat(SkyBlockData.getRepository(BonusItemRarity.class).findAll(), is(empty()));
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
        collectTable(cells, "profile", profileStats);

        for (int slot = 0; slot < profileStats.getArmor().size(); slot++) {
            int index = slot;
            profileStats.getArmor()
                .get(slot)
                .ifPresent(itemData -> collectTable(cells, String.format("armor/%d:%s", index, itemData.getItem().getId()), itemData));
        }

        ConcurrentList<AccessoryData> accessories = profileStats.getAccessoryBag().getAccessories();

        for (int index = 0; index < accessories.size(); index++) {
            AccessoryData accessoryData = accessories.get(index);
            collectTable(cells, String.format("accessory/%03d:%s", index, accessoryData.getAccessory().getId()), accessoryData);
        }

        return cells;
    }

    private static void collectTable(Map<String, Double> cells, String owner, StatData<?> statData) {
        statData.getStats().forEach((bucket, statEntries) -> statEntries.forEach((statModel, data) -> {
            String path = String.format("%s/%s/%s", owner, bucket.name(), statModel.getId());
            record(cells, path + "/base", data.getBase());
            record(cells, path + "/bonus", data.getBonus());
        }));
    }

    private static int writtenCells(StatData<?> statData) {
        return statData.getStats()
            .stream()
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
