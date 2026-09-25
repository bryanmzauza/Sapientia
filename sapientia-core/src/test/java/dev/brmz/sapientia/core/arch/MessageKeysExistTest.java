package dev.brmz.sapientia.core.arch;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every literal message key passed to {@code Messages} in core sources must
 * exist in both bundled catalogues; otherwise players see
 * {@code [missing: key]}.
 */
final class MessageKeysExistTest {

    private static final Pattern KEY_CALL = Pattern.compile(
            "(?<!Placeholder)\\.(?:component|plain|hasKey)\\(\\s*\"([a-z0-9_.\\-]+)\"");

    @Test
    void literalKeysExistInBothCatalogues() throws IOException {
        Set<String> used = new TreeSet<>();
        Path sources = Path.of("src/main/java");
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                Matcher m = KEY_CALL.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (m.find()) {
                    used.add(m.group(1));
                }
            }
        }
        assertThat(used).as("sanity: scanner found message keys").hasSizeGreaterThan(50);

        for (String locale : List.of("en", "pt_BR")) {
            Map<String, Object> catalog = load(locale);
            List<String> missing = used.stream().filter(key -> !(lookup(catalog, key) instanceof String)).toList();
            assertThat(missing).as("keys missing from lang/%s.yml", locale).isEmpty();
        }
    }

    private static Map<String, Object> load(String locale) throws IOException {
        try (InputStream in = MessageKeysExistTest.class.getClassLoader()
                .getResourceAsStream("lang/" + locale + ".yml")) {
            assertThat(in).isNotNull();
            return new Yaml().load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @SuppressWarnings("unchecked")
    private static Object lookup(Map<String, Object> catalog, String dottedKey) {
        Object node = catalog;
        for (String part : dottedKey.split("\\.")) {
            if (!(node instanceof Map<?, ?> map)) return null;
            node = ((Map<String, Object>) map).get(part);
        }
        return node;
    }
}
