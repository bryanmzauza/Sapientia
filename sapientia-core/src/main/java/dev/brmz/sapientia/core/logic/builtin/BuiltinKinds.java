package dev.brmz.sapientia.core.logic.builtin;

import java.util.Locale;
import java.util.Map;

import dev.brmz.sapientia.api.logic.LogicValue;
import dev.brmz.sapientia.api.logic.LogicValueType;
import dev.brmz.sapientia.core.logic.LogicNodeExecutor;
import org.jetbrains.annotations.NotNull;

/**
 * Built-in {@link LogicNodeExecutor} implementations for the small,
 * non-Turing instruction set shipped with 1.3.0 (T-302).
 *
 * <p>Each kind is a pure function of {@code params + inputs} (and, for
 * {@code memory_*} / {@code log}, of the shared {@code LogicContext}). Outputs
 * are returned as a port-keyed map so downstream edges can read them.
 */
public final class BuiltinKinds {

    private BuiltinKinds() { }

    public static void registerAll(@NotNull Map<String, LogicNodeExecutor> registry) {
        registry.put("constant", BuiltinKinds::constant);
        registry.put("add", arith(Long::sum));
        registry.put("sub", arith((a, b) -> a - b));
        registry.put("mul", arith((a, b) -> a * b));
        registry.put("compare", BuiltinKinds::compare);
        registry.put("and", boolBin((a, b) -> a && b));
        registry.put("or",  boolBin((a, b) -> a || b));
        registry.put("not", BuiltinKinds::notNode);
        registry.put("branch", BuiltinKinds::branch);
        registry.put("memory_read", BuiltinKinds::memoryRead);
        registry.put("memory_write", BuiltinKinds::memoryWrite);
        registry.put("tick_counter", BuiltinKinds::tickCounter);
        registry.put("log", BuiltinKinds::log);
        registry.put("noop", (id, p, in, ctx) -> Map.of());
    }

    @FunctionalInterface
    private interface LongBinOp { long apply(long a, long b); }

    @FunctionalInterface
    private interface BoolBinOp { boolean apply(boolean a, boolean b); }

    private static @NotNull LogicNodeExecutor arith(@NotNull LongBinOp op) {
        return (id, params, inputs, ctx) -> {
            long a = inputOrZero(inputs, "a").asInt();
            long b = inputOrZero(inputs, "b").asInt();
            return Map.of("out", LogicValue.ofInt(op.apply(a, b)));
        };
    }

    private static @NotNull LogicNodeExecutor boolBin(@NotNull BoolBinOp op) {
        return (id, params, inputs, ctx) -> {
            boolean a = inputOrFalse(inputs, "a").asBool();
            boolean b = inputOrFalse(inputs, "b").asBool();
            return Map.of("out", LogicValue.ofBool(op.apply(a, b)));
        };
    }

    private static @NotNull Map<String, LogicValue> constant(@NotNull String id,
                                                             @NotNull Map<String, LogicValue> params,
                                                             @NotNull Map<String, LogicValue> inputs,
                                                             @NotNull dev.brmz.sapientia.core.logic.LogicContext ctx) {
        LogicValue value = params.get("value");
        if (value != null) {
            return Map.of("out", value);
        }
        // Fall back to type+raw split for YAML round-trips that store both.
        LogicValue typeParam = params.get("type");
        LogicValue rawParam = params.get("raw");
        if (rawParam == null) {
            return Map.of("out", LogicValue.ofInt(0L));
        }
        if (typeParam != null) {
            try {
                LogicValueType t = LogicValueType.valueOf(typeParam.asString().toUpperCase(Locale.ROOT));
                return Map.of("out", switch (t) {
                    case INT -> LogicValue.ofInt(rawParam.asInt());
                    case BOOL -> LogicValue.ofBool(rawParam.asBool());
                    case STRING -> LogicValue.ofString(rawParam.asString());
                });
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return Map.of("out", rawParam);
    }

    private static @NotNull Map<String, LogicValue> compare(@NotNull String id,
                                                            @NotNull Map<String, LogicValue> params,
                                                            @NotNull Map<String, LogicValue> inputs,
                                                            @NotNull dev.brmz.sapientia.core.logic.LogicContext ctx) {
        long a = inputOrZero(inputs, "a").asInt();
        long b = inputOrZero(inputs, "b").asInt();
        LogicValue opParam = params.get("op");
        String op = opParam == null ? "eq" : opParam.asString().toLowerCase(Locale.ROOT);
        boolean result = switch (op) {
            case "eq" -> a == b;
            case "ne" -> a != b;
            case "lt" -> a < b;
            case "le" -> a <= b;
            case "gt" -> a > b;
            case "ge" -> a >= b;
            default -> false;
        };
        return Map.of("out", LogicValue.ofBool(result));
    }

    private static @NotNull Map<String, LogicValue> notNode(@NotNull String id,
                                                            @NotNull Map<String, LogicValue> params,
                                                            @NotNull Map<String, LogicValue> inputs,
                                                            @NotNull dev.brmz.sapientia.core.logic.LogicContext ctx) {
        boolean a = inputOrFalse(inputs, "a").asBool();
        return Map.of("out", LogicValue.ofBool(!a));
    }

    private static @NotNull Map<String, LogicValue> branch(@NotNull String id,
                                                           @NotNull Map<String, LogicValue> params,
                                                           @NotNull Map<String, LogicValue> inputs,
                                                           @NotNull dev.brmz.sapientia.core.logic.LogicContext ctx) {
        boolean cond = inputOrFalse(inputs, "cond").asBool();
        LogicValue chosen = cond ? inputs.get("whenTrue") : inputs.get("whenFalse");
        if (chosen == null) {
            chosen = LogicValue.ofInt(0L);
        }
        return Map.of("out", chosen);
    }

    private static @NotNull Map<String, LogicValue> memoryRead(@NotNull String id,
                                                               @NotNull Map<String, LogicValue> params,
                                                               @NotNull Map<String, LogicValue> inputs,
                                                               @NotNull dev.brmz.sapientia.core.logic.LogicContext ctx) {
        LogicValue keyParam = params.get("key");
        if (keyParam == null) {
            return Map.of("out", LogicValue.ofInt(0L));
        }
        LogicValue stored = ctx.readMemory(keyParam.asString());
        return Map.of("out", stored != null ? stored : LogicValue.ofInt(0L));
    }

    private static @NotNull Map<String, LogicValue> memoryWrite(@NotNull String id,
                                                                @NotNull Map<String, LogicValue> params,
                                                                @NotNull Map<String, LogicValue> inputs,
                                                                @NotNull dev.brmz.sapientia.core.logic.LogicContext ctx) {
        LogicValue keyParam = params.get("key");
        LogicValue value = inputs.get("value");
        if (keyParam != null && value != null) {
            ctx.writeMemory(keyParam.asString(), value);
        }
        return Map.of();
    }

    private static @NotNull Map<String, LogicValue> tickCounter(@NotNull String id,
                                                                @NotNull Map<String, LogicValue> params,
                                                                @NotNull Map<String, LogicValue> inputs,
                                                                @NotNull dev.brmz.sapientia.core.logic.LogicContext ctx) {
        return Map.of("out", LogicValue.ofInt(ctx.tick()));
    }

    private static @NotNull Map<String, LogicValue> log(@NotNull String id,
                                                        @NotNull Map<String, LogicValue> params,
                                                        @NotNull Map<String, LogicValue> inputs,
                                                        @NotNull dev.brmz.sapientia.core.logic.LogicContext ctx) {
        LogicValue prefix = params.get("prefix");
        LogicValue value = inputs.get("value");
        StringBuilder sb = new StringBuilder();
        if (prefix != null) {
            sb.append(prefix.asString()).append('=');
        }
        sb.append(value != null ? value.asString() : "<none>");
        ctx.log(id, sb.toString());
        return Map.of();
    }

    private static @NotNull LogicValue inputOrZero(@NotNull Map<String, LogicValue> inputs, @NotNull String port) {
        LogicValue value = inputs.get(port);
        return value != null ? value : LogicValue.ofInt(0L);
    }

    private static @NotNull LogicValue inputOrFalse(@NotNull Map<String, LogicValue> inputs, @NotNull String port) {
        LogicValue value = inputs.get(port);
        return value != null ? value : LogicValue.ofBool(false);
    }
}
