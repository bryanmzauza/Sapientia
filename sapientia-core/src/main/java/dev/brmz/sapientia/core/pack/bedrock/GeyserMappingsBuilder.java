package dev.brmz.sapientia.core.pack.bedrock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;

/**
 * Renders the Geyser custom item mappings (format version 2) that make Bedrock
 * clients show Sapientia items with their own icon and name.
 *
 * <p>Each Java item that carries a {@code minecraft:item_model} component is
 * matched by that model. Output shape:
 * <pre>
 * {
 *   "format_version": 2,
 *   "items": {
 *     "minecraft:iron_ingot": [
 *       { "type": "definition",
 *         "model": "sapientia:copper_ingot",
 *         "bedrock_identifier": "sapientia:copper_ingot",
 *         "display_name": "Copper Ingot",
 *         "bedrock_options": { "icon": "sapientia.copper_ingot" } }
 *     ]
 *   }
 * }
 * </pre>
 * The icon name matches the {@code textures/item_texture.json} entries in the
 * bundled Bedrock pack. The file belongs in Geyser's {@code custom_mappings}
 * folder, not inside the {@code .mcpack}.
 */
public final class GeyserMappingsBuilder {

    /**
     * One mapped item.
     *
     * @param id          namespaced id, also used as the item model and Bedrock identifier
     * @param baseItem    namespaced vanilla item the stack is built on, e.g. {@code minecraft:iron_ingot}
     * @param displayName plain-text fallback name
     */
    public record Entry(@NotNull String id, @NotNull String baseItem, @NotNull String displayName) {}

    private GeyserMappingsBuilder() {}

    public static @NotNull String render(@NotNull List<Entry> entries) {
        Map<String, List<Entry>> byBase = new TreeMap<>();
        for (Entry entry : entries) {
            byBase.computeIfAbsent(entry.baseItem(), k -> new ArrayList<>()).add(entry);
        }

        StringBuilder sb = new StringBuilder(entries.size() * 240 + 64);
        sb.append("{\n  \"format_version\": 2,\n  \"items\": {");
        boolean firstBase = true;
        for (Map.Entry<String, List<Entry>> group : byBase.entrySet()) {
            sb.append(firstBase ? "\n" : ",\n");
            firstBase = false;
            sb.append("    \"").append(escape(group.getKey())).append("\": [");
            List<Entry> sorted = new ArrayList<>(group.getValue());
            sorted.sort(Comparator.comparing(Entry::id));
            boolean first = true;
            for (Entry entry : sorted) {
                sb.append(first ? "\n" : ",\n");
                first = false;
                String id = escape(entry.id());
                sb.append("      {\n")
                        .append("        \"type\": \"definition\",\n")
                        .append("        \"model\": \"").append(id).append("\",\n")
                        .append("        \"bedrock_identifier\": \"").append(id).append("\",\n")
                        .append("        \"display_name\": \"").append(escape(entry.displayName())).append("\",\n")
                        .append("        \"bedrock_options\": { \"icon\": \"")
                        .append(escape(iconName(entry.id()))).append("\" }\n")
                        .append("      }");
            }
            sb.append("\n    ]");
        }
        sb.append("\n  }\n}\n");
        return sb.toString();
    }

    /** Bedrock icon key for an item id: {@code sapientia:copper_ingot -> sapientia.copper_ingot}. */
    public static @NotNull String iconName(@NotNull String id) {
        return id.replace(':', '.').replace('/', '_');
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
