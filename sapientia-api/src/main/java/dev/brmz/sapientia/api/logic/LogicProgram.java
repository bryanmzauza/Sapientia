package dev.brmz.sapientia.api.logic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

/**
 * A non-Turing programmable rule expressed as a directed acyclic graph
 * of {@link LogicNode}s connected by {@link LogicEdge}s (T-302 / 1.3.0).
 *
 * <p>Programs are pure data — the actual scheduling, compilation and execution
 * happen in {@code sapientia-core}. The DAG is intentionally non-Turing: every
 * node executes at most once per tick and there are no loops or recursion, so
 * a program's worst-case cost per tick is bounded by {@code O(|V|+|E|)}.
 */
public record LogicProgram(@NotNull String name,
                           @NotNull List<LogicNode> nodes,
                           @NotNull List<LogicEdge> edges) {

    public LogicProgram {
        if (name.isBlank()) {
            throw new IllegalArgumentException("program name must not be blank");
        }
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        Set<String> ids = new HashSet<>();
        for (LogicNode node : nodes) {
            if (!ids.add(node.id())) {
                throw new IllegalArgumentException("duplicate node id: " + node.id());
            }
        }
    }
}
