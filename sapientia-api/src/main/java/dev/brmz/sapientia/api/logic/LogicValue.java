package dev.brmz.sapientia.api.logic;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Tagged primitive shuttled between {@link LogicNode} ports (T-302 / 1.3.0).
 *
 * <p>Three concrete shapes — {@code INT} ({@code long}), {@code BOOL} ({@code boolean})
 * and {@code STRING} ({@code String}) — chosen so YAML round-trips remain trivial and
 * deterministic. Construct via {@link #ofInt(long)}, {@link #ofBool(boolean)}, or
 * {@link #ofString(String)}; never with the canonical constructor directly.
 */
public final class LogicValue {

    private final LogicValueType type;
    private final long intValue;
    private final boolean boolValue;
    private final String stringValue;

    private LogicValue(@NotNull LogicValueType type,
                       long intValue,
                       boolean boolValue,
                       String stringValue) {
        this.type = Objects.requireNonNull(type, "type");
        this.intValue = intValue;
        this.boolValue = boolValue;
        this.stringValue = stringValue;
    }

    public static @NotNull LogicValue ofInt(long value) {
        return new LogicValue(LogicValueType.INT, value, false, null);
    }

    public static @NotNull LogicValue ofBool(boolean value) {
        return new LogicValue(LogicValueType.BOOL, 0L, value, null);
    }

    public static @NotNull LogicValue ofString(@NotNull String value) {
        return new LogicValue(LogicValueType.STRING, 0L, false, Objects.requireNonNull(value, "value"));
    }

    public @NotNull LogicValueType type() {
        return type;
    }

    public long asInt() {
        return switch (type) {
            case INT -> intValue;
            case BOOL -> boolValue ? 1L : 0L;
            case STRING -> {
                try {
                    yield Long.parseLong(stringValue);
                } catch (NumberFormatException ex) {
                    yield 0L;
                }
            }
        };
    }

    public boolean asBool() {
        return switch (type) {
            case BOOL -> boolValue;
            case INT -> intValue != 0L;
            case STRING -> Boolean.parseBoolean(stringValue);
        };
    }

    public @NotNull String asString() {
        return switch (type) {
            case STRING -> stringValue;
            case INT -> Long.toString(intValue);
            case BOOL -> Boolean.toString(boolValue);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LogicValue other)) return false;
        if (other.type != type) return false;
        return switch (type) {
            case INT -> other.intValue == intValue;
            case BOOL -> other.boolValue == boolValue;
            case STRING -> Objects.equals(other.stringValue, stringValue);
        };
    }

    @Override
    public int hashCode() {
        return switch (type) {
            case INT -> Long.hashCode(intValue);
            case BOOL -> Boolean.hashCode(boolValue);
            case STRING -> stringValue.hashCode();
        };
    }

    @Override
    public String toString() {
        return type + ":" + asString();
    }
}
