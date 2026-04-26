package dev.brmz.sapientia.core.pack.bedrock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import dev.brmz.sapientia.api.item.SapientiaItem;
import dev.brmz.sapientia.core.item.ItemRegistry;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Builds the Geyser {@code item_mappings.json} that lets Bedrock clients see
 * Sapientia items as the right base item with the right CMD (T-208 / 1.0.0).
 *
 * <p>Output schema follows Geyser's {@code custom-mappings} project:
 * <pre>
 * {
 *   "format_version": "1",
 *   "items": {
 *     "minecraft:&lt;baseMaterial&gt;": [
 *       { "name": "&lt;namespace&gt;:&lt;path&gt;",
 *         "custom_model_data": &lt;cmd&gt;,
 *         "display_name": "&lt;path&gt;" }
 *     ]
 *   }
 * }
 * </pre>
 *
 * <p>Items with {@code customModelData() == 0} are skipped — Geyser uses CMD as
 * the discriminator, so a zero-CMD entry would collide with the vanilla item.
 */
public final class GeyserMappingsBuilder {

    private final ItemRegistry items;

    public GeyserMappingsBuilder(@NotNull ItemRegistry items) {
        this.items = items;
    }

    /** Writes {@code mappings/sapientia_items.json} under {@code targetDir}. */
    public @NotNull Path write(@NotNull Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Path output = targetDir.resolve("sapientia_items.json");
        Files.writeString(output, render(), StandardCharsets.UTF_8);
        return output;
    }

    /** Renders the mappings JSON to a string. Visible for tests. */
    public @NotNull String render() {
        // Group by base material (Bedrock parent identifier).
        Map<String, java.util.List<SapientiaItem>> byBase = new TreeMap<>();
        for (Map.Entry<NamespacedKey, SapientiaItem> e : items.allSapientiaItems().entrySet()) {
            SapientiaItem item = e.getValue();
            if (item.customModelData() <= 0) continue;
            String base = "minecraft:" + item.baseMaterial().getKey().getKey();
            byBase.computeIfAbsent(base, k -> new java.util.ArrayList<>()).add(item);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"format_version\": \"1\",\n  \"items\": {");
        boolean firstBase = true;
        for (Map.Entry<String, java.util.List<SapientiaItem>> entry : byBase.entrySet()) {
            if (!firstBase) sb.append(',');
            firstBase = false;
            sb.append("\n    \"").append(escape(entry.getKey())).append("\": [");
            boolean first = true;
            // Sort by CMD for deterministic output.
            entry.getValue().sort((a, b) -> Integer.compare(a.customModelData(), b.customModelData()));
            for (SapientiaItem item : entry.getValue()) {
                if (!first) sb.append(',');
                first = false;
                sb.append("\n      {")
                        .append("\n        \"name\": \"").append(escape(item.id().toString())).append("\",")
                        .append("\n        \"custom_model_data\": ").append(item.customModelData()).append(',')
                        .append("\n        \"display_name\": \"").append(escape(item.id().getKey())).append('"')
                        .append("\n      }");
            }
            sb.append("\n    ]");
        }
        sb.append("\n  }\n}\n");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
