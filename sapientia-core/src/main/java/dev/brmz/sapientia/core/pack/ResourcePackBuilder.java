package dev.brmz.sapientia.core.pack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import dev.brmz.sapientia.api.Version;
import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.item.ItemRegistry;
import dev.brmz.sapientia.core.pack.bedrock.BedrockPackConstants;
import dev.brmz.sapientia.core.pack.bedrock.GeyserMappingsBuilder;
import dev.brmz.sapientia.core.pack.bedrock.LangFileWriter;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the Java and Bedrock resource packs from the assets bundled in the
 * plugin jar ({@link BundledPack}) plus operator overrides.
 *
 * <p>Java: {@code plugins/Sapientia/sapientia-resources.zip}. Files placed under
 * {@code plugins/Sapientia/pack/} replace the bundled file with the same path,
 * so operators can reskin any item without rebuilding the plugin.
 * {@code pack.mcmeta} is always generated from the configured pack format.
 *
 * <p>Bedrock: {@code plugins/Sapientia/sapientia-bedrock.mcpack} (textures,
 * icons and {@code .lang} files) plus the Geyser item mappings in
 * {@code plugins/Sapientia/geyser/sapientia_items.json}. When Geyser is
 * installed on the same server both files are also copied into its
 * {@code packs/} and {@code custom_mappings/} folders.
 */
public final class ResourcePackBuilder {

    /** Resource pack format of Minecraft 26.3 (97.1). */
    public static final int DEFAULT_PACK_FORMAT = 97;

    /** First format that uses {@code min_format}/{@code max_format} (Minecraft 1.21.9). */
    static final int FIRST_RANGED_FORMAT = 65;

    static final String OVERRIDES_README = "README.txt";
    private static final String MAPPINGS_FILE = "sapientia_items.json";
    private static final LocalDateTime ENTRY_TIME = LocalDateTime.of(2020, 1, 1, 0, 0);

    /** Result of {@link #buildJavaPack()}. */
    public record JavaPackResult(@NotNull Path pack, @NotNull String sha1, int overrides) {}

    /** Result of {@link #buildBedrockPack()}; {@code geyserFolder} is set when installed into Geyser. */
    public record BedrockPackResult(@NotNull Path pack, @Nullable Path mappings, @Nullable Path geyserFolder) {}

    private final Logger logger;
    private final Path dataDir;
    private final BundledPack bundled;
    private final int packFormat;
    private final Version version;
    private @Nullable Messages messages;
    private @Nullable ItemRegistry itemRegistry;
    private Supplier<Optional<Path>> geyserFolder = Optional::empty;

    public ResourcePackBuilder(@NotNull Logger logger, @NotNull Path dataDir, @NotNull BundledPack bundled,
                               int configuredPackFormat, @NotNull Version version) {
        this.logger = logger;
        this.dataDir = dataDir;
        this.bundled = bundled;
        this.version = version;
        this.packFormat = effectivePackFormat(configuredPackFormat, logger);
    }

    /**
     * Formats below 65 predate the ranged {@code min_format}/{@code max_format}
     * fields and cannot target this Minecraft version. Configs written by older
     * Sapientia releases shipped {@code 32}, so fall back to the current format.
     */
    static int effectivePackFormat(int configured, @NotNull Logger logger) {
        if (configured < FIRST_RANGED_FORMAT) {
            logger.warning("resource-pack.pack-format " + configured + " is too old for this Minecraft "
                    + "version; using " + DEFAULT_PACK_FORMAT + " instead. Update config.yml to silence this.");
            return DEFAULT_PACK_FORMAT;
        }
        return configured;
    }

    /** Required for Bedrock {@code .lang} files and mapping display names. */
    public void setMessages(@Nullable Messages messages) {
        this.messages = messages;
    }

    /** Required for the Geyser item mappings. */
    public void setItemRegistry(@Nullable ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    /** Locates Geyser's data folder, if Geyser is installed on this server. */
    public void setGeyserFolder(@NotNull Supplier<Optional<Path>> geyserFolder) {
        this.geyserFolder = geyserFolder;
    }

    public int packFormat() {
        return packFormat;
    }

    /** Directory whose files override the bundled Java assets. */
    public @NotNull Path overridesDir() {
        return dataDir.resolve("pack");
    }

    /** Ensures the overrides directory exists and explains how to use it. */
    public void prepare() throws IOException {
        Path dir = overridesDir();
        Files.createDirectories(dir);
        Path readme = dir.resolve(OVERRIDES_README);
        if (!Files.exists(readme)) {
            Files.writeString(readme, """
                    Files in this folder replace the bundled Sapientia assets when you run
                    /sapientia pack build java. Mirror the path inside the pack, e.g.:

                      assets/sapientia/textures/item/wrench.png
                      assets/sapientia/textures/block/macerator_front.png

                    pack.mcmeta is generated automatically; set resource-pack.pack-format
                    in config.yml instead of editing it here.
                    """, StandardCharsets.UTF_8);
        }
    }

    // --- Java -------------------------------------------------------------------

    public @NotNull JavaPackResult buildJavaPack() throws IOException {
        prepare();
        Map<String, Path> overrides = collectOverrides();
        Path output = dataDir.resolve("sapientia-resources.zip");
        String sha1 = writeZipAtomically(output, zip -> {
            putEntry(zip, "pack.mcmeta", packMcmeta(packFormat).getBytes(StandardCharsets.UTF_8));
            for (String path : bundled.pathsUnder(BundledPack.JAVA_ROOT)) {
                if (path.equals("pack.mcmeta") || overrides.containsKey(path)) continue;
                try (InputStream in = bundled.open(BundledPack.JAVA_ROOT + path)) {
                    putEntry(zip, path, in.readAllBytes());
                }
            }
            for (Map.Entry<String, Path> override : overrides.entrySet()) {
                putEntry(zip, override.getKey(), Files.readAllBytes(override.getValue()));
            }
        });
        logger.info("Sapientia resource pack written to " + output + " (" + overrides.size()
                + " override(s), SHA-1 " + sha1 + ")");
        return new JavaPackResult(output, sha1, overrides.size());
    }

    /** Operator files keyed by pack-relative path, excluding generated and legacy files. */
    private Map<String, Path> collectOverrides() throws IOException {
        Path root = overridesDir();
        Map<String, Path> out = new TreeMap<>();
        if (!Files.isDirectory(root)) {
            return out;
        }
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                String rel = root.relativize(file).toString().replace('\\', '/');
                // bedrock/ was the staging folder of Sapientia <= 1.10; pack.mcmeta is generated.
                if (rel.equals(OVERRIDES_README) || rel.equals("pack.mcmeta") || rel.startsWith("bedrock/")) {
                    continue;
                }
                out.put(rel, file);
            }
        }
        return out;
    }

    static @NotNull String packMcmeta(int format) {
        return """
                {
                  "pack": {
                    "description": "Sapientia resources",
                    "min_format": %d,
                    "max_format": %d
                  }
                }
                """.formatted(format, format);
    }

    // --- Bedrock ----------------------------------------------------------------

    public @NotNull BedrockPackResult buildBedrockPack() throws IOException {
        Files.createDirectories(dataDir);
        Path pack = dataDir.resolve("sapientia-bedrock.mcpack");
        writeZipAtomically(pack, zip -> {
            putEntry(zip, "manifest.json", bedrockManifestJson().getBytes(StandardCharsets.UTF_8));
            for (String path : bundled.pathsUnder(BundledPack.BEDROCK_ROOT)) {
                try (InputStream in = bundled.open(BundledPack.BEDROCK_ROOT + path)) {
                    putEntry(zip, path, in.readAllBytes());
                }
            }
            if (messages != null) {
                for (Map.Entry<String, String> lang : new LangFileWriter(messages).render().entrySet()) {
                    putEntry(zip, "texts/" + lang.getKey(), lang.getValue().getBytes(StandardCharsets.UTF_8));
                }
            } else {
                logger.warning("buildBedrockPack: Messages not wired — skipping .lang generation.");
            }
        });

        Path mappings = null;
        if (itemRegistry != null) {
            mappings = dataDir.resolve("geyser").resolve(MAPPINGS_FILE);
            Files.createDirectories(mappings.getParent());
            Files.writeString(mappings, GeyserMappingsBuilder.render(mappingEntries()), StandardCharsets.UTF_8);
        } else {
            logger.warning("buildBedrockPack: ItemRegistry not wired — skipping Geyser mappings.");
        }

        Path installed = null;
        Optional<Path> geyser = geyserFolder.get();
        if (geyser.isPresent()) {
            installed = geyser.get();
            copyInto(pack, installed.resolve("packs"));
            if (mappings != null) {
                copyInto(mappings, installed.resolve("custom_mappings"));
            }
        }
        logger.info("Sapientia Bedrock pack written to " + pack
                + (installed != null ? " and installed into " + installed : ""));
        return new BedrockPackResult(pack, mappings, installed);
    }

    /** Items that have both a Java item model and a Bedrock icon. */
    @NotNull List<GeyserMappingsBuilder.Entry> mappingEntries() {
        List<GeyserMappingsBuilder.Entry> out = new ArrayList<>();
        if (itemRegistry == null) {
            return out;
        }
        Set<String> models = bundled.itemModelIds();
        Set<String> icons = bundled.bedrockIconIds();
        for (ItemRegistry.ItemDefinition def : itemRegistry.all().values()) {
            NamespacedKey key = NamespacedKey.fromString(def.id());
            if (key == null || !ItemRegistry.BUNDLED_NAMESPACE.equals(key.getNamespace())) continue;
            if (!models.contains(key.getKey()) || !icons.contains(key.getKey())) continue;
            String name = messages != null && messages.hasKey(def.displayNameKey())
                    ? messages.plain(def.displayNameKey())
                    : key.getKey();
            String base = "minecraft:" + itemRegistry.materialOf(def.id()).getKey().getKey();
            out.add(new GeyserMappingsBuilder.Entry(key.toString(), base, name));
        }
        return out;
    }

    private @NotNull String bedrockManifestJson() {
        // Bedrock caches packs by UUID + version, so the version must move with every release.
        int[] v = {version.major(), version.minor(), version.patch()};
        int[] mev = BedrockPackConstants.MIN_ENGINE_VERSION;
        return """
                {
                  "format_version": 2,
                  "header": {
                    "name": "%s",
                    "description": "%s",
                    "uuid": "%s",
                    "version": [%d, %d, %d],
                    "min_engine_version": [%d, %d, %d]
                  },
                  "modules": [
                    {
                      "type": "resources",
                      "uuid": "%s",
                      "version": [%d, %d, %d]
                    }
                  ]
                }
                """.formatted(
                BedrockPackConstants.PACK_NAME,
                BedrockPackConstants.PACK_DESCRIPTION,
                BedrockPackConstants.HEADER_UUID,
                v[0], v[1], v[2],
                mev[0], mev[1], mev[2],
                BedrockPackConstants.MODULE_UUID,
                v[0], v[1], v[2]);
    }

    // --- Zip helpers ----------------------------------------------------------------

    @FunctionalInterface
    private interface ZipWriter {
        void write(ZipOutputStream zip) throws IOException;
    }

    /** Writes to a temp file then moves it into place; returns the SHA-1 of the archive. */
    private static String writeZipAtomically(Path output, ZipWriter writer) throws IOException {
        Path temp = output.resolveSibling(output.getFileName() + ".tmp");
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
        try (OutputStream out = new DigestOutputStream(Files.newOutputStream(temp), sha1);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            writer.write(zip);
        }
        Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return HexFormat.of().formatHex(sha1.digest());
    }

    private static void putEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        // Fixed timestamps keep the archive (and its SHA-1) stable across rebuilds.
        entry.setTimeLocal(ENTRY_TIME);
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }

    private static void copyInto(Path file, Path dir) throws IOException {
        Files.createDirectories(dir);
        Files.copy(file, dir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
    }
}
