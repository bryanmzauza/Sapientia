package dev.brmz.sapientia.core.pack.bedrock;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

final class GeyserMappingsBuilderTest {

    @Test
    @SuppressWarnings("unchecked")
    void rendersFormatVersionTwoGroupedByBaseItem() {
        String json = GeyserMappingsBuilder.render(List.of(
                new GeyserMappingsBuilder.Entry("sapientia:tin_ingot", "minecraft:iron_ingot", "Tin Ingot"),
                new GeyserMappingsBuilder.Entry("sapientia:copper_ingot", "minecraft:iron_ingot", "Copper Ingot"),
                new GeyserMappingsBuilder.Entry("sapientia:macerator", "minecraft:grindstone", "Macerator")));

        Map<String, Object> root = new Yaml().load(json);
        assertThat(root.get("format_version")).isEqualTo(2);
        Map<String, List<Map<String, Object>>> items = (Map<String, List<Map<String, Object>>>) root.get("items");
        assertThat(items).containsOnlyKeys("minecraft:iron_ingot", "minecraft:grindstone");

        List<Map<String, Object>> ingots = items.get("minecraft:iron_ingot");
        assertThat(ingots).extracting(m -> m.get("model"))
                .containsExactly("sapientia:copper_ingot", "sapientia:tin_ingot");
        Map<String, Object> copper = ingots.get(0);
        assertThat(copper).containsEntry("type", "definition")
                .containsEntry("bedrock_identifier", "sapientia:copper_ingot")
                .containsEntry("display_name", "Copper Ingot");
        assertThat((Map<String, Object>) copper.get("bedrock_options"))
                .containsEntry("icon", "sapientia.copper_ingot");
    }

    @Test
    void escapesDisplayNames() {
        String json = GeyserMappingsBuilder.render(List.of(
                new GeyserMappingsBuilder.Entry("sapientia:x", "minecraft:stick", "Say \"hi\"\\")));
        Map<String, Object> root = new Yaml().load(json);
        assertThat(json).contains("Say \\\"hi\\\"\\\\");
        assertThat(root).containsKey("items");
    }

    @Test
    void emptyInputIsValidJson() {
        Map<String, Object> root = new Yaml().load(GeyserMappingsBuilder.render(List.of()));
        assertThat(root.get("items")).isEqualTo(Map.of());
    }
}
