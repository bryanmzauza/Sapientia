package dev.brmz.sapientia.api.logic;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

/**
 * One vertex of a {@link LogicProgram} DAG (T-302 / 1.3.0).
 *
 * <p>{@code id} is unique within a program and used to reference the node from
 * {@link LogicEdge}s. {@code kind} resolves a built-in executor at compile time
 * (see the registry in {@code sapientia-core}). {@code params} are static values
 * baked into the node — distinct from runtime port inputs.
 */
public record LogicNode(@NotNull String id,
                        @NotNull String kind,
                        @NotNull Map<String, LogicValue> params) {

    public LogicNode {
        if (id.isBlank()) {
            throw new IllegalArgumentException("node id must not be blank");
        }
        if (kind.isBlank()) {
            throw new IllegalArgumentException("node kind must not be blank");
        }
        params = Map.copyOf(params);
    }
}
