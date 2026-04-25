package dev.brmz.sapientia.api.logic;

import org.jetbrains.annotations.NotNull;

/**
 * A directed connection from {@code (fromNode, fromPort)} to {@code (toNode, toPort)}
 * in a {@link LogicProgram} (T-302 / 1.3.0). The compiler rejects programs whose
 * edges form cycles or whose endpoints reference unknown node ids.
 */
public record LogicEdge(@NotNull String fromNode,
                        @NotNull String fromPort,
                        @NotNull String toNode,
                        @NotNull String toPort) {

    public LogicEdge {
        if (fromNode.isBlank()) {
            throw new IllegalArgumentException("fromNode must not be blank");
        }
        if (fromPort.isBlank()) {
            throw new IllegalArgumentException("fromPort must not be blank");
        }
        if (toNode.isBlank()) {
            throw new IllegalArgumentException("toNode must not be blank");
        }
        if (toPort.isBlank()) {
            throw new IllegalArgumentException("toPort must not be blank");
        }
        if (fromNode.equals(toNode)) {
            throw new IllegalArgumentException("self-loop edges are not allowed: " + fromNode);
        }
    }
}
