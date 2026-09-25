package dev.brmz.sapientia.core.pack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Read-only view of the resource-pack assets shipped inside the plugin jar
 * under {@code pack/}. The file list comes from {@code pack/index.txt}, which
 * the build generates from {@code src/main/resources/pack}.
 *
 * <p>Layout: {@code java/} holds the Java Edition pack root and {@code bedrock/}
 * the Bedrock Edition pack root. Both are produced by
 * {@code scripts/textures/generate_textures.py}.
 */
public final class BundledPack {

    public static final String INDEX = "pack/index.txt";
    public static final String JAVA_ROOT = "java/";
    public static final String BEDROCK_ROOT = "bedrock/";
    private static final String ITEM_DEFINITIONS = JAVA_ROOT + "assets/sapientia/items/";
    private static final String BEDROCK_ICONS = BEDROCK_ROOT + "textures/items/";

    private final Function<String, @Nullable InputStream> opener;
    private final List<String> paths;

    public BundledPack(@NotNull Function<String, @Nullable InputStream> opener) {
        this.opener = opener;
        this.paths = readIndex(opener);
    }

    /** Loads assets through the given class loader (the plugin's, in production). */
    public static @NotNull BundledPack fromClassLoader(@NotNull ClassLoader loader) {
        return new BundledPack(loader::getResourceAsStream);
    }

    private static List<String> readIndex(Function<String, @Nullable InputStream> opener) {
        InputStream in = opener.apply(INDEX);
        if (in == null) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    out.add(line.strip());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + INDEX, e);
        }
        return Collections.unmodifiableList(out);
    }

    /** Every bundled path, relative to {@code pack/}. */
    public @NotNull List<String> paths() {
        return paths;
    }

    /** Paths under the given root ({@link #JAVA_ROOT} or {@link #BEDROCK_ROOT}), with the root stripped. */
    public @NotNull List<String> pathsUnder(@NotNull String root) {
        List<String> out = new ArrayList<>();
        for (String path : paths) {
            if (path.startsWith(root)) {
                out.add(path.substring(root.length()));
            }
        }
        return out;
    }

    /** Opens a bundled file; {@code path} is relative to {@code pack/}. */
    public @NotNull InputStream open(@NotNull String path) throws IOException {
        InputStream in = opener.apply("pack/" + path);
        if (in == null) {
            throw new IOException("Bundled pack file missing from jar: pack/" + path);
        }
        return in;
    }

    /** Ids (without namespace) that have a Java item definition, e.g. {@code macerator}. */
    public @NotNull Set<String> itemModelIds() {
        return idsIn(ITEM_DEFINITIONS, ".json");
    }

    /** Ids (without namespace) that have a Bedrock inventory icon. */
    public @NotNull Set<String> bedrockIconIds() {
        return idsIn(BEDROCK_ICONS, ".png");
    }

    private Set<String> idsIn(String dir, String extension) {
        Set<String> out = new LinkedHashSet<>();
        for (String path : paths) {
            if (path.startsWith(dir) && path.endsWith(extension) && path.indexOf('/', dir.length()) < 0) {
                out.add(path.substring(dir.length(), path.length() - extension.length()));
            }
        }
        return out;
    }
}
