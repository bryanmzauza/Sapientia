package dev.brmz.sapientia.core.logic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dev.brmz.sapientia.api.logic.LogicEdge;
import dev.brmz.sapientia.api.logic.LogicNode;
import dev.brmz.sapientia.api.logic.LogicProgram;
import dev.brmz.sapientia.api.logic.LogicValue;
import dev.brmz.sapientia.api.logic.LogicValueType;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * SnakeYAML-backed serialiser for {@link LogicProgram} (T-302 / 1.3.0).
 *
 * <p>The on-disk format is intentionally human-readable so the in-game viewer
 * and the visual editor planned for 1.9.0 can share the same source of truth.
 *
 * <pre>
 * name: example
 * nodes:
 *   - id: c1
 *     kind: constant
 *     params: { type: INT, raw: 42 }
 *   - id: out
 *     kind: log
 *     params: { prefix: "result" }
 * edges:
 *   - from: c1
 *     fromPort: out
 *     to: out
 *     toPort: value
 * </pre>
 */
public final class LogicYaml {

    public @NotNull String dump(@NotNull LogicProgram program) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("name", program.name());

        List<Map<String, Object>> nodes = new ArrayList<>(program.nodes().size());
        for (LogicNode node : program.nodes()) {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", node.id());
            n.put("kind", node.kind());
            if (!node.params().isEmpty()) {
                Map<String, Object> p = new LinkedHashMap<>();
                for (Map.Entry<String, LogicValue> e : node.params().entrySet()) {
                    p.put(e.getKey(), encodeValue(e.getValue()));
                }
                n.put("params", p);
            }
            nodes.add(n);
        }
        root.put("nodes", nodes);

        List<Map<String, Object>> edges = new ArrayList<>(program.edges().size());
        for (LogicEdge edge : program.edges()) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("from", edge.fromNode());
            e.put("fromPort", edge.fromPort());
            e.put("to", edge.toNode());
            e.put("toPort", edge.toPort());
            edges.add(e);
        }
        root.put("edges", edges);

        DumperOptions opt = new DumperOptions();
        opt.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opt.setIndent(2);
        opt.setPrettyFlow(true);
        return new Yaml(opt).dump(root);
    }

    @SuppressWarnings("unchecked")
    public @NotNull LogicProgram parse(@NotNull String yaml) {
        Object loaded = new Yaml().load(yaml);
        if (!(loaded instanceof Map)) {
            throw new IllegalArgumentException("logic YAML root must be a map");
        }
        Map<String, Object> root = (Map<String, Object>) loaded;
        Object nameObj = root.get("name");
        if (!(nameObj instanceof String name) || name.isBlank()) {
            throw new IllegalArgumentException("logic YAML missing 'name'");
        }

        List<LogicNode> nodes = new ArrayList<>();
        Object nodesObj = root.get("nodes");
        if (nodesObj instanceof List<?> rawNodes) {
            for (Object raw : rawNodes) {
                if (!(raw instanceof Map<?, ?> rawMap)) {
                    throw new IllegalArgumentException("each node must be a map");
                }
                Map<String, Object> n = (Map<String, Object>) rawMap;
                String id = stringOrThrow(n, "id");
                String kind = stringOrThrow(n, "kind");
                Map<String, LogicValue> params = new LinkedHashMap<>();
                Object paramsObj = n.get("params");
                if (paramsObj instanceof Map<?, ?> rawParams) {
                    for (Map.Entry<?, ?> entry : rawParams.entrySet()) {
                        params.put(String.valueOf(entry.getKey()), decodeValue(entry.getValue()));
                    }
                }
                nodes.add(new LogicNode(id, kind, params));
            }
        }

        List<LogicEdge> edges = new ArrayList<>();
        Object edgesObj = root.get("edges");
        if (edgesObj instanceof List<?> rawEdges) {
            for (Object raw : rawEdges) {
                if (!(raw instanceof Map<?, ?> rawMap)) {
                    throw new IllegalArgumentException("each edge must be a map");
                }
                Map<String, Object> e = (Map<String, Object>) rawMap;
                edges.add(new LogicEdge(
                        stringOrThrow(e, "from"),
                        stringOrThrow(e, "fromPort"),
                        stringOrThrow(e, "to"),
                        stringOrThrow(e, "toPort")));
            }
        }

        return new LogicProgram(name, nodes, edges);
    }

    private static @NotNull String stringOrThrow(@NotNull Map<String, Object> map, @NotNull String key) {
        Object v = map.get(key);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("missing or non-string field: " + key);
        }
        return s;
    }

    private static @NotNull Object encodeValue(@NotNull LogicValue value) {
        // Encode as a typed map so types survive round-trips with no ambiguity.
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", value.type().name());
        map.put("raw", switch (value.type()) {
            case INT -> value.asInt();
            case BOOL -> value.asBool();
            case STRING -> value.asString();
        });
        return map;
    }

    @SuppressWarnings("unchecked")
    private static @NotNull LogicValue decodeValue(Object raw) {
        if (raw instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            Object typeObj = map.get("type");
            Object valueObj = map.get("raw");
            if (typeObj instanceof String typeStr) {
                LogicValueType type;
                try {
                    type = LogicValueType.valueOf(typeStr.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException("unknown logic value type: " + typeStr);
                }
                return switch (type) {
                    case INT -> LogicValue.ofInt(asLong(valueObj));
                    case BOOL -> LogicValue.ofBool(asBool(valueObj));
                    case STRING -> LogicValue.ofString(valueObj == null ? "" : valueObj.toString());
                };
            }
        }
        // Bare scalar — infer type.
        if (raw instanceof Boolean b) {
            return LogicValue.ofBool(b);
        }
        if (raw instanceof Number n) {
            return LogicValue.ofInt(n.longValue());
        }
        return LogicValue.ofString(raw == null ? "" : raw.toString());
    }

    private static long asLong(Object raw) {
        if (raw instanceof Number n) return n.longValue();
        if (raw instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) { }
        }
        return 0L;
    }

    private static boolean asBool(Object raw) {
        if (raw instanceof Boolean b) return b;
        if (raw instanceof Number n) return n.longValue() != 0L;
        if (raw instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }
}
