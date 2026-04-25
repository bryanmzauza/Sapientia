package dev.brmz.sapientia.core.pack;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

/**
 * Minimal Java resource pack builder (T-164 / 0.5.0).
 *
 * <p>Writes a well-formed {@code pack.mcmeta} and zips the on-disk
 * {@code plugins/Sapientia/pack/} directory into {@code sapientia-resources.zip}.
 * Operators drop custom textures/models into that directory; the pipeline
 * stitches them together without demanding a full Minecraft asset tree.
 *
 * <p>The full {@code ItemModel} generation loop tracked under T-164 will extend
 * this class once custom-model-data becomes part of the public item API.
 */
public final class ResourcePackBuilder {

    private final Logger logger;
    private final Path packDir;
    private final int packFormat;

    public ResourcePackBuilder(@NotNull Logger logger, @NotNull Path packDir, int packFormat) {
        this.logger = logger;
        this.packDir = packDir;
        this.packFormat = packFormat;
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
}
