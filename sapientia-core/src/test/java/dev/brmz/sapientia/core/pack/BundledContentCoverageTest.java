package dev.brmz.sapientia.core.pack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every catalogue entry with a display name (item, block, metal, component,
 * android block and upgrade, and mineral sections of the language files) must ship with a
 * Java model and a Bedrock icon, and every bundled model must belong to such
 * an entry. Regenerate with {@code scripts/textures/generate_textures.py}.
 */
final class BundledContentCoverageTest {

    private static final List<List<String>> SECTIONS = List.of(
            List.of("item"), List.of("block"), List.of("metal"), List.of("component"),
            List.of("android", "block"), List.of("android", "upgrade"), List.of("mineral"));

    private final BundledPack pack = BundledPack.fromClassLoader(getClass().getClassLoader());

    @Test
    void everyNamedEntryHasAModelAndAnIcon() {
        Set<String> named = namedIds("en");
        assertThat(named).hasSizeGreaterThan(200);

        List<String> missing = new ArrayList<>();
        for (String id : named) {
            if (!pack.itemModelIds().contains(id) || !pack.bedrockIconIds().contains(id)) {
                missing.add(id);
            }
        }
        assertThat(missing).as("entries without bundled textures").isEmpty();
    }

    @Test
    void everyBundledModelHasANamedEntry() {
        assertThat(namedIds("en")).containsAll(pack.itemModelIds());
    }

    @Test
    void bothLocalesNameTheSameEntries() {
        assertThat(namedIds("pt_BR")).isEqualTo(namedIds("en"));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> namedIds(String locale) {
        Map<String, Object> catalog;
        try (InputStream in = BundledContentCoverageTest.class.getClassLoader()
                .getResourceAsStream("lang/" + locale + ".yml")) {
            assertThat(in).as("lang/%s.yml on classpath", locale).isNotNull();
            catalog = new Yaml().load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        Set<String> ids = new TreeSet<>();
        for (List<String> path : SECTIONS) {
            Object node = catalog;
            for (String part : path) {
                node = node instanceof Map<?, ?> map ? map.get(part) : null;
            }
            if (!(node instanceof Map<?, ?> section)) continue;
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) section).entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> fields && fields.get("name") instanceof String) {
                    ids.add(entry.getKey());
                }
            }
        }
        return ids;
    }
}
