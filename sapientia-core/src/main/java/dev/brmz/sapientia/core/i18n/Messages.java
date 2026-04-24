package dev.brmz.sapientia.core.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.Yaml;

/**
 * Catalog-backed message resolver. English is the default and fallback locale; additional
 * locales override entries key-by-key. See docs/i18n-strategy.md.
 */
public final class Messages {

    private static final String DEFAULT_LOCALE = "en";
    private static final String[] BUNDLED_LOCALES = {"en", "pt_BR"};

    private final Logger logger;
    private final MiniMessage mini = MiniMessage.miniMessage();
    private final Map<String, Map<String, String>> catalogs = new HashMap<>();
    private volatile String activeLocale = DEFAULT_LOCALE;

    public Messages(Logger logger) {
        this.logger = logger;
    }

    /** Load bundled catalogs from the plugin classpath. */
    public void loadBundled(@NotNull Plugin plugin) {
        for (String locale : BUNDLED_LOCALES) {
            try (InputStream in = plugin.getResource("lang/" + locale + ".yml")) {
                if (in == null) {
                    logger.warning("Missing bundled catalog: lang/" + locale + ".yml");
                    continue;
                }
                Map<String, Object> raw = new Yaml().load(new InputStreamReader(in, StandardCharsets.UTF_8));
                Map<String, String> flat = new HashMap<>();
                flatten("", raw, flat);
                catalogs.put(locale, flat);
                logger.fine(() -> "Loaded catalog " + locale + " with " + flat.size() + " keys.");
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to load catalog " + locale, e);
            }
        }
    }

    public void setActiveLocale(@NotNull String locale) {
        if (!catalogs.containsKey(locale)) {
            logger.warning("Requested locale '" + locale + "' is not loaded. Falling back to " + DEFAULT_LOCALE + ".");
            this.activeLocale = DEFAULT_LOCALE;
            return;
        }
        this.activeLocale = locale;
    }

    public @NotNull String activeLocale() {
        return activeLocale;
    }

    /** Look up a key as a resolved Adventure component, substituting &lt;placeholders&gt;. */
    public @NotNull Component component(@NotNull String key, @NotNull TagResolver... resolvers) {
        String template = resolve(key);
        return mini.deserialize(template, resolvers);
    }

    /** Convenience overload for string placeholders. */
    public @NotNull Component component(@NotNull String key, @NotNull Map<String, String> placeholders) {
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(e -> (TagResolver) Placeholder.parsed(e.getKey(), e.getValue()))
                .toArray(TagResolver[]::new);
        return component(key, resolvers);
    }

    /** Plain-text rendering. Useful for logs and console feedback. */
    public @NotNull String plain(@NotNull String key, @NotNull TagResolver... resolvers) {
        return PlainTextComponentSerializer.plainText().serialize(component(key, resolvers));
    }

    public boolean hasKey(@NotNull String key) {
        return catalogs.getOrDefault(activeLocale, Collections.emptyMap()).containsKey(key)
                || catalogs.getOrDefault(DEFAULT_LOCALE, Collections.emptyMap()).containsKey(key);
    }

    private @NotNull String resolve(@NotNull String key) {
        Map<String, String> primary = catalogs.getOrDefault(activeLocale, Collections.emptyMap());
        String value = primary.get(key);
        if (value != null) {
            return value;
        }
        Map<String, String> fallback = catalogs.getOrDefault(DEFAULT_LOCALE, Collections.emptyMap());
        value = fallback.get(key);
        if (value != null) {
            return value;
        }
        logger.warning(() -> "Missing i18n key: " + key);
        return "<red>[missing: " + key + "]</red>";
    }

    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> node, Map<String, String> out) {
        if (node == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : node.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> map) {
                flatten(key, (Map<String, Object>) map, out);
            } else if (value != null) {
                out.put(key, String.valueOf(value));
            }
        }
    }

    /** Returns the list of locales bundled with the plugin. */
    public static Locale[] bundledLocales() {
        return new Locale[] {Locale.ENGLISH, Locale.of("pt", "BR")};
    }
}
