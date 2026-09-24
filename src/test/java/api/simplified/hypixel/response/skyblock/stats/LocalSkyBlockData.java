package api.simplified.hypixel.response.skyblock.stats;

import api.simplified.github.ManifestIndex;
import api.simplified.skyblock.SkyBlockData;
import api.simplified.skyblock.model.Item;
import com.google.gson.Gson;
import dev.simplified.collection.Concurrent;
import dev.simplified.collection.ConcurrentList;
import dev.simplified.gson.GsonSettings;
import dev.simplified.persistence.JpaConfig;
import dev.simplified.persistence.JpaModel;
import dev.simplified.persistence.JpaSession;
import dev.simplified.persistence.exception.JpaException;
import dev.simplified.persistence.source.DocumentOrigin;
import dev.simplified.persistence.source.DocumentSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A SkyBlock session whose reference corpus is a checkout on disk rather than the GitHub Contents
 * API.
 * <p>
 * The production connect is bound to GitHub and cannot be repointed, so this builds its own
 * {@link JpaConfig} and registers it with the same process-wide manager
 * {@link SkyBlockData#getRepository(Class)} resolves against - which is what lets the whole
 * {@code stats} package run unchanged with no request leaving the machine. Unauthenticated
 * GitHub reads are capped at sixty an hour and one connect spends about forty-two of them, so a
 * suite that connects at all has to connect to disk.
 * <p>
 * The manager is static, so a session opened here is visible to every other test class in the same
 * JVM. Whoever connects must {@link #disconnect(JpaSession)} before yielding.
 */
final class LocalSkyBlockData {

    /**
     * System property naming the {@code skyblock} checkout, for a runner whose working directory is
     * not the module.
     */
    static final @NotNull String ROOT_PROPERTY = "skyblock.corpus.root";

    private static final @NotNull String MANIFEST_PATH = "data/v1/index.json";

    private LocalSkyBlockData() {
    }

    /**
     * The corpus checkout, empty when neither the property nor the sibling directory holds one.
     * <p>
     * The corpus is the {@code data/v1} tree the {@code skyblock} module ships, so the sibling
     * candidate is that module's checkout - where a full workspace puts it. A clone of this module
     * alone has no corpus and every test that needs one skips rather than fails.
     *
     * @return the checkout root, empty when no manifest is readable under either candidate
     */
    static @NotNull Optional<Path> findCorpus() {
        String declared = System.getProperty(ROOT_PROPERTY);

        Path root = (declared == null || declared.isBlank())
            ? Path.of("..", "skyblock")
            : Path.of(declared);

        root = root.toAbsolutePath().normalize();

        return Files.isReadable(root.resolve(MANIFEST_PATH)) ? Optional.of(root) : Optional.empty();
    }

    /**
     * The revision the corpus catalogue was taken at, which is half of what makes a golden file
     * reproducible.
     *
     * @param root the checkout root
     * @return the catalogue's revision, empty when it declares none
     */
    static @NotNull Optional<String> corpusCommitSha(@NotNull Path root) {
        String revision = readManifest(root).getRevision();
        return revision.isEmpty() ? Optional.empty() : Optional.of(revision);
    }

    /**
     * Models this build declares that the checkout's catalogue carries no document for.
     * <p>
     * Every type is read during the connect, so one uncovered model fails the whole thing. It means
     * the reference models and the corpus are of different vintages - normally a {@code skyblock}
     * pin behind the corpus - which no amount of local setup fixes.
     *
     * @param root the checkout root
     * @return the uncovered document names, empty when the two agree
     */
    static @NotNull ConcurrentList<String> uncoveredModels(@NotNull Path root) {
        ManifestIndex manifest = readManifest(root);

        return JpaModel.resolveModels(Item.class)
            .stream()
            .map(JpaModel::documentOf)
            .filter(name -> manifest.layersOf(name).isEmpty())
            .collect(Concurrent.toList());
    }

    /**
     * Opens a session reading every reference table out of the checkout.
     *
     * @param root the checkout root
     * @return the registered session, which the caller owns and must shut down
     */
    static @NotNull JpaSession connect(@NotNull Path root) {
        return SkyBlockData.getSessionManager().connect(new JpaConfig(
            JpaModel.resolveModels(Item.class),
            new DocumentSource(new Checkout(root), SkyBlockData.corpusSettings().create())
        ));
    }

    /**
     * Closes a session and unregisters it, so a later test class sees no active session.
     *
     * @param session the session to close, null when the connect never happened
     */
    static void disconnect(@Nullable JpaSession session) {
        if (session != null)
            SkyBlockData.getSessionManager().shutdown(session);
    }

    private static @NotNull ManifestIndex readManifest(@NotNull Path root) {
        Gson gson = GsonSettings.defaults().create();
        return gson.fromJson(read(root.resolve(MANIFEST_PATH), MANIFEST_PATH), ManifestIndex.class);
    }

    private static @NotNull String read(@NotNull Path path, @NotNull String reported) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new JpaException(exception, "Unable to read '%s' from the local corpus", reported);
        }
    }

    /**
     * A checkout answering the same two questions a published corpus does.
     */
    private record Checkout(@NotNull Path root) implements DocumentOrigin {

        @Override
        public @NotNull ConcurrentList<String> layersOf(@NotNull String name) {
            return readManifest(this.root())
                .layersOf(name)
                .stream()
                .map(ManifestIndex.Layer::path)
                .collect(Concurrent.toUnmodifiableList());
        }

        @Override
        public @NotNull String read(@NotNull String path) {
            return LocalSkyBlockData.read(this.root().resolve(path), path);
        }

    }

}
