package dev.brmz.sapientia.core.pack;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/** Consistency checks over the generated assets shipped in {@code pack/}. */
final class BundledPackTest {

    private final BundledPack pack = BundledPack.fromClassLoader(getClass().getClassLoader());
    private final Set<String> paths = new HashSet<>(pack.paths());

    @Test
    void indexListsJavaAndBedrockRoots() {
        assertThat(pack.pathsUnder(BundledPack.JAVA_ROOT)).contains("pack.png");
        assertThat(pack.pathsUnder(BundledPack.BEDROCK_ROOT))
                .contains("pack_icon.png", "textures/item_texture.json");
        assertThat(pack.itemModelIds()).contains("macerator", "copper_ingot", "wrench");
    }

    @Test
    void everyItemDefinitionResolvesToAModelAndItsTextures() throws IOException {
        for (String id : pack.itemModelIds()) {
            Map<String, Object> definition = json("java/assets/sapientia/items/" + id + ".json");
            @SuppressWarnings("unchecked")
            String modelRef = (String) ((Map<String, Object>) definition.get("model")).get("model");
            String modelPath = "java/assets/" + resourcePath(modelRef, "models") + ".json";
            assertThat(paths).as("model for %s", id).contains(modelPath);

            @SuppressWarnings("unchecked")
            Map<String, String> textures = (Map<String, String>) json(modelPath).get("textures");
            assertThat(textures).as("textures of %s", modelRef).isNotEmpty();
            for (String texture : textures.values()) {
                assertThat(paths).as("texture %s", texture)
                        .contains("java/assets/" + resourcePath(texture, "textures") + ".png");
            }
        }
    }

    @Test
    void bedrockAtlasMatchesIcons() throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Map<String, String>> data = (Map<String, Map<String, String>>)
                json("bedrock/textures/item_texture.json").get("texture_data");
        assertThat(data).hasSize(pack.bedrockIconIds().size());
        for (String id : pack.bedrockIconIds()) {
            Map<String, String> entry = data.get("sapientia." + id);
            assertThat(entry).as("atlas entry for %s", id).isNotNull();
            assertThat(paths).contains("bedrock/" + entry.get("textures") + ".png");
        }
    }

    /** {@code sapientia:item/foo} under {@code models} -> {@code sapientia/models/item/foo}. */
    private static String resourcePath(String ref, String kind) {
        String[] parts = ref.split(":", 2);
        return parts[0] + "/" + kind + "/" + parts[1];
    }

    private Map<String, Object> json(String path) throws IOException {
        try (InputStream in = pack.open(path)) {
            return new Yaml().load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
