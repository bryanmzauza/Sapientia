package dev.brmz.sapientia.core.progression;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.brmz.sapientia.api.mining.Mineral;
import dev.brmz.sapientia.api.progression.Era;
import dev.brmz.sapientia.content.ContentEras;
import dev.brmz.sapientia.content.mining.MineralCatalog;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/** Every named catalogue entry belongs to an era, and every era has a name and summary in both languages. */
final class CatalogueErasTest {

    @Test
    void everyNamedEntryHasAnEra() throws IOException {
        Map<String, Object> en = load("en");
        Set<String> mineralItems = new HashSet<>();
        for (Mineral mineral : MineralCatalog.minerals()) {
            mineralItems.add(mineral.fragmentItem().getKey());
            mineralItems.add(mineral.tailingsItem().getKey());
        }
        List<String> missing = new ArrayList<>();
        for (List<String> path : List.of(List.of("item"), List.of("block"), List.of("metal"), List.of("component"),
                List.of("android", "block"), List.of("android", "upgrade"))) {
            for (String id : section(en, path).keySet()) {
                if (ContentEras.find(id) == null) missing.add(id);
            }
        }
        for (String id : section(en, List.of("mineral")).keySet()) {
            if (!mineralItems.contains(id)) missing.add(id);
        }
        assertThat(missing).as("entries without an era").isEmpty();
    }

    @Test
    void everyEraIsNamedInBothLanguages() throws IOException {
        for (String locale : List.of("en", "pt_BR")) {
            Map<String, Object> catalog = load(locale);
            for (Era era : Era.values()) {
                String base = era.nameKey().substring(0, era.nameKey().length() - ".name".length());
                Map<String, Object> entry = section(catalog, List.of(base.split("\\.")));
                assertThat(entry.get("name")).as("%s %s", locale, era.nameKey()).isInstanceOf(String.class);
                assertThat(entry.get("summary")).as("%s %s", locale, era.summaryKey()).isInstanceOf(String.class);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Map<String, Object> root, List<String> path) {
        Object node = root;
        for (String part : path) {
            node = node instanceof Map<?, ?> map ? map.get(part) : null;
        }
        return node instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static Map<String, Object> load(String locale) throws IOException {
        try (InputStream in = CatalogueErasTest.class.getClassLoader().getResourceAsStream("lang/" + locale + ".yml")) {
            assertThat(in).isNotNull();
            return new Yaml().load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
