package dev.brmz.sapientia.core.pack.bedrock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dev.brmz.sapientia.core.i18n.Messages;
import dev.brmz.sapientia.core.i18n.TextAdapter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

/**
 * Translates Sapientia's MiniMessage-flavoured {@code lang/*.yml} catalogs into
 * Bedrock {@code .lang} files (T-205b / 1.0.0).
 *
 * <p>Bedrock's lang files are a flat {@code key=value} text format: there's no
 * MiniMessage parser, so we render every entry to a Component and serialise it
 * with {@link TextAdapter#toPlainBedrock} so legacy {@code §} colour codes
 * survive while the rest is dropped.
 *
 * <p>Locale codes are mapped from Sapientia's BCP-47 forms ({@code pt_BR}) to
 * Bedrock's underscore-uppercase forms ({@code pt_BR}). The two happen to
 * coincide for our bundled locales today; the mapping table exists so future
 * locales can be normalised without touching the writer call-site.
 */
public final class LangFileWriter {

    private final Messages messages;

    public LangFileWriter(@NotNull Messages messages) {
        this.messages = messages;
    }

    /**
     * Writes one {@code <bedrockLocale>.lang} file per loaded locale into
     * {@code targetDir/texts/} and returns the list of paths written.
     */
    public @NotNull List<Path> writeAll(@NotNull Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        List<Path> written = new ArrayList<>();
        MiniMessage mm = MiniMessage.miniMessage();

        for (String locale : sortedLocales()) {
            Map<String, String> raw = messages.catalogFor(locale);
            if (raw.isEmpty()) continue;
            Map<String, String> rendered = new TreeMap<>();
            for (Map.Entry<String, String> e : raw.entrySet()) {
                String value = TextAdapter.toPlainBedrock(mm.deserialize(e.getValue()));
                rendered.put(e.getKey(), value);
            }
            Path out = targetDir.resolve(toBedrockLocale(locale) + ".lang");
            try (var w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
                for (Map.Entry<String, String> e : rendered.entrySet()) {
                    w.write(e.getKey());
                    w.write('=');
                    // Bedrock .lang doesn't permit raw newlines inside values.
                    w.write(e.getValue().replace("\r", "").replace('\n', ' '));
                    w.write('\t');
                    w.write('#');
                    w.newLine();
                }
            }
            written.add(out);
        }
        return written;
    }

    /** Maps Sapientia locale codes to Bedrock's expected file-name format. */
    public static @NotNull String toBedrockLocale(@NotNull String sapientiaLocale) {
        // Sapientia uses "en" / "pt_BR" today; Bedrock accepts both forms,
        // but the canonical names are "en_US" and "pt_BR".
        return switch (sapientiaLocale) {
            case "en" -> "en_US";
            default -> sapientiaLocale;
        };
    }

    private @NotNull List<String> sortedLocales() {
        List<String> locales = new ArrayList<>(messages.loadedLocales());
        locales.sort(String::compareTo);
        return locales;
    }
}
