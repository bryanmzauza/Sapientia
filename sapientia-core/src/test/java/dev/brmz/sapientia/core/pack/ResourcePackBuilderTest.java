package dev.brmz.sapientia.core.pack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dev.brmz.sapientia.api.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

final class ResourcePackBuilderTest {

    private static final Logger LOG = Logger.getLogger("ResourcePackBuilderTest");

    @TempDir
    Path dataDir;

    private static BundledPack fakePack() {
        Map<String, String> files = new HashMap<>();
        files.put("java/pack.png", "png");
        files.put("java/assets/sapientia/items/wrench.json", "{}");
        files.put("java/assets/sapientia/textures/item/wrench.png", "bundled");
        files.put("bedrock/pack_icon.png", "icon");
        files.put("bedrock/textures/items/wrench.png", "bedrock-wrench");
        files.put("bedrock/textures/item_texture.json", "{}");
        String index = String.join("\n", files.keySet());
        return new BundledPack(path -> {
            String content = path.equals(BundledPack.INDEX) ? index : files.get(path.substring("pack/".length()));
            return content == null ? null : new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        });
    }

    private ResourcePackBuilder builder(int format) {
        return new ResourcePackBuilder(LOG, dataDir, fakePack(), format, new Version(1, 11, 0));
    }

    private static Map<String, String> unzip(Path zip) throws IOException {
        Map<String, String> out = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                out.put(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return out;
    }

    @Test
    void javaPackContainsBundledAssetsAndGeneratedMcmeta() throws IOException {
        ResourcePackBuilder.JavaPackResult result = builder(97).buildJavaPack();

        Map<String, String> zip = unzip(result.pack());
        assertThat(zip).containsKeys("pack.mcmeta", "pack.png", "assets/sapientia/items/wrench.json");
        assertThat(zip.get("pack.mcmeta")).contains("\"min_format\": 97").contains("\"max_format\": 97");
        assertThat(result.sha1()).hasSize(40);
    }

    @Test
    void legacyPackFormatFallsBackToCurrentDefault() {
        assertThat(builder(32).packFormat()).isEqualTo(ResourcePackBuilder.DEFAULT_PACK_FORMAT);
        assertThat(builder(98).packFormat()).isEqualTo(98);
    }

    @Test
    void operatorFilesOverrideBundledOnesAndLegacyFilesAreIgnored() throws IOException {
        Path overrides = dataDir.resolve("pack");
        Files.createDirectories(overrides.resolve("assets/sapientia/textures/item"));
        Files.writeString(overrides.resolve("assets/sapientia/textures/item/wrench.png"), "custom");
        Files.writeString(overrides.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":32}}");
        Files.createDirectories(overrides.resolve("bedrock"));
        Files.writeString(overrides.resolve("bedrock/manifest.json"), "stale staging file");

        ResourcePackBuilder.JavaPackResult result = builder(97).buildJavaPack();

        Map<String, String> zip = unzip(result.pack());
        assertThat(result.overrides()).isEqualTo(1);
        assertThat(zip.get("assets/sapientia/textures/item/wrench.png")).isEqualTo("custom");
        assertThat(zip.get("pack.mcmeta")).contains("min_format");
        assertThat(zip).doesNotContainKeys("bedrock/manifest.json", ResourcePackBuilder.OVERRIDES_README);
    }

    @Test
    void rebuildingWithoutChangesKeepsTheSameHash() throws IOException {
        ResourcePackBuilder builder = builder(97);
        String first = builder.buildJavaPack().sha1();
        String second = builder.buildJavaPack().sha1();
        assertThat(second).isEqualTo(first);
    }

    @Test
    void bedrockPackCarriesVersionedManifestAndTextures() throws IOException {
        ResourcePackBuilder.BedrockPackResult result = builder(97).buildBedrockPack();

        Map<String, String> zip = unzip(result.pack());
        assertThat(zip).containsKeys("manifest.json", "pack_icon.png", "textures/items/wrench.png",
                "textures/item_texture.json");
        assertThat(zip.get("manifest.json")).contains("\"version\": [1, 11, 0]");
        assertThat(zip.keySet()).noneMatch(name -> name.startsWith("mappings/"));
        assertThat(result.geyserFolder()).isNull();
    }

    @Test
    void bedrockPackIsInstalledIntoGeyserWhenPresent() throws IOException {
        Path geyser = dataDir.resolve("Geyser-Spigot");
        ResourcePackBuilder builder = builder(97);
        builder.setGeyserFolder(() -> Optional.of(geyser));

        ResourcePackBuilder.BedrockPackResult result = builder.buildBedrockPack();

        assertThat(result.geyserFolder()).isEqualTo(geyser);
        assertThat(geyser.resolve("packs/sapientia-bedrock.mcpack")).exists();
    }
}
