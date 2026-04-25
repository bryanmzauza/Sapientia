package dev.brmz.sapientia.core.pack;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.logging.Logger;

import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.item.ItemRegistry;
import dev.brmz.sapientia.core.pack.bedrock.BedrockPackConstants;
import dev.brmz.sapientia.core.pack.bedrock.GeyserMappingsBuilder;
import dev.brmz.sapientia.core.pack.bedrock.LangFileWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resource pack builder for Java + Bedrock (T-164 / T-207).
 *
 * <p>Writes a well-formed {@code pack.mcmeta} and zips the on-disk
 * {@code plugins/Sapientia/pack/} directory into {@code sapientia-resources.zip}.
 * Operators drop custom textures/models into that directory; the pipeline
 * stitches them together without demanding a full Minecraft asset tree.
 *
 * <p>{@link #buildBedrockPack()} produces a sibling {@code sapientia-bedrock.mcpack}
 * containing a Bedrock {@code manifest.json}, generated {@code .lang} files for
 * each loaded locale, and a Geyser {@code item_mappings.json}. {@link Messages}
 * + {@link ItemRegistry} are optional dependencies — when {@code null}, the
 * Bedrock pipeline degrades gracefully (no lang / no mappings).
 */
public final class ResourcePackBuilder {

    private final Logger logger;
    private final Path packDir;
    private final int packFormat;
    private @Nullable Messages messages;
    private @Nullable ItemRegistry itemRegistry;

    public ResourcePackBuilder(@NotNull Logger logger, @NotNull Path packDir, int packFormat) {
        this.logger = logger;
        this.packDir = packDir;
        this.packFormat = packFormat;
    }

    /** Optional injection point: required for {@link #buildBedrockPack()} to emit lang files. */
    public void setMessages(@Nullable Messages messages) {
        this.messages = messages;
    }

    /** Optional injection point: required for {@link #buildBedrockPack()} to emit Geyser mappings. */
    public void setItemRegistry(@Nullable ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    /** Ensures the pack directory exists and seeds {@code pack.mcmeta} when missing. */
    public void prepare() throws IOException {
        Files.createDirectories(packDir);
        Path mcmeta = packDir.resolve("pack.mcmeta");
        if (!Files.exists(mcmeta)) {
            String content = """
                    {
                      "pack": {
                        "pack_format": %d,
                        "description": "Sapientia custom content"
                      }
                    }
                    """.formatted(packFormat);
            Files.writeString(mcmeta, content, StandardCharsets.UTF_8);
        }
        Path assets = packDir.resolve("assets").resolve("sapientia");
        Files.createDirectories(assets);
    }

    /**
     * Zips the pack directory into {@code sapientia-resources.zip} beside it and
     * returns the produced path.
     */
    public @NotNull Path buildJavaPack() throws IOException {
        prepare();
        Path output = packDir.resolveSibling("sapientia-resources.zip");
        try (OutputStream out = Files.newOutputStream(output);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            zipDir(packDir, packDir, zip);
        }
        logger.info("Sapientia resource pack written to " + output);
        return output;
    }

    private static void zipDir(Path root, Path current, ZipOutputStream zip) throws IOException {
        try (var stream = Files.newDirectoryStream(current)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    zipDir(root, entry, zip);
                    continue;
                }
                String name = root.relativize(entry).toString().replace('\\', '/');
                zip.putNextEntry(new ZipEntry(name));
                Files.copy(entry, zip);
                zip.closeEntry();
            }
        }
    }

    /**
     * Builds a {@code sapientia-bedrock.mcpack} (T-207) containing a Bedrock
     * manifest, generated {@code .lang} files for each loaded locale, and a
     * Geyser {@code item_mappings.json}. The pack staging directory lives at
     * {@code packDir/bedrock/}.
     */
    public @NotNull Path buildBedrockPack() throws IOException {
        Path stage = packDir.resolve("bedrock");
        Files.createDirectories(stage);
        Path texts = stage.resolve("texts");
        Files.createDirectories(texts);
        Files.writeString(stage.resolve("manifest.json"), bedrockManifestJson(),
                StandardCharsets.UTF_8);

        if (messages != null) {
            new LangFileWriter(messages).writeAll(texts);
        } else {
            logger.warning("buildBedrockPack: Messages not wired — skipping .lang generation.");
        }
        if (itemRegistry != null) {
            new GeyserMappingsBuilder(itemRegistry).write(stage.resolve("mappings"));
        } else {
            logger.warning("buildBedrockPack: ItemRegistry not wired — skipping Geyser mappings.");
        }

        Path output = packDir.resolveSibling("sapientia-bedrock.mcpack");
        try (OutputStream out = Files.newOutputStream(output);
             ZipOutputStream zip = new ZipOutputStream(out)) {
            zipDir(stage, stage, zip);
        }
        logger.info("Sapientia Bedrock pack written to " + output);
        return output;
    }

    private static String bedrockManifestJson() {
        int[] v = BedrockPackConstants.VERSION;
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
}
