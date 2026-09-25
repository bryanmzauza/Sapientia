package dev.brmz.sapientia.core.pack.bedrock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
     * Renders one {@code <bedrockLocale>.lang} file per loaded locale, keyed by
     * file name (for example {@code en_US.lang}), in locale order.
     */
    public @NotNull Map<String, String> render() {
        Map<String, String> files = new LinkedHashMap<>();
        MiniMessage mm = MiniMessage.miniMessage();

        for (String locale : sortedLocales()) {
            Map<String, String> raw = messages.catalogFor(locale);
            if (raw.isEmpty()) continue;
            Map<String, String> rendered = new TreeMap<>();
            for (Map.Entry<String, String> e : raw.entrySet()) {
                String value = TextAdapter.toPlainBedrock(mm.deserialize(e.getValue()));
                rendered.put(e.getKey(), value);
            }
            StringBuilder out = new StringBuilder(rendered.size() * 48);
            for (Map.Entry<String, String> e : rendered.entrySet()) {
                // Bedrock .lang doesn't permit raw newlines inside values.
                out.append(e.getKey()).append('=')
                        .append(e.getValue().replace("\r", "").replace('\n', ' '))
                        .append("\t#").append('\n');
            }
            files.put(toBedrockLocale(locale) + ".lang", out.toString());
        }
        return files;
    }

    /**
     * Writes the files from {@link #render()} into {@code targetDir} and returns
     * the paths written.
     */
    public @NotNull List<Path> writeAll(@NotNull Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        List<Path> written = new ArrayList<>();
        for (Map.Entry<String, String> file : render().entrySet()) {
            Path out = targetDir.resolve(file.getKey());
            Files.writeString(out, file.getValue(), StandardCharsets.UTF_8);
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
