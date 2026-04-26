package dev.brmz.sapientia.core.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import dev.brmz.sapientia.api.logic.LogicEdge;
import dev.brmz.sapientia.api.logic.LogicNode;
import dev.brmz.sapientia.api.logic.LogicProgram;
import org.jetbrains.annotations.NotNull;

/**
 * Compiles a {@link LogicProgram} into a {@link CompiledProgram} with a
 * deterministic topological execution order (T-302 / 1.3.0). The compiler is
 * pure: same input ⇒ same output, including identical step ordering.
 *
 * <p>Validation rules:
 * <ul>
 *   <li>Every {@link LogicNode#kind()} must resolve via {@link LogicNodeKind}.</li>
 *   <li>Every {@link LogicEdge} endpoint must reference an existing node id.</li>
 *   <li>No two edges may target the same {@code (toNode, toPort)} pair.</li>
 *   <li>The graph must be acyclic — Kahn's algorithm is used to detect cycles.</li>
 * </ul>
 *
 * <p>Tie-breaking inside Kahn's algorithm uses lexicographic node-id order so that
 * topological sort is deterministic across runs and machines.
 */
public final class LogicCompiler {

    public @NotNull CompiledProgram compile(@NotNull LogicProgram program) {
        Map<String, LogicNode> byId = new HashMap<>();
        for (LogicNode node : program.nodes()) {
            byId.put(node.id(), node);
        }

        // Validate edges + build adjacency.
        Map<String, List<LogicEdge>> incoming = new HashMap<>();
        Map<String, List<String>> outgoingTargets = new HashMap<>();
        Set<String> seenInputPorts = new HashSet<>();
        for (LogicEdge edge : program.edges()) {
            if (!byId.containsKey(edge.fromNode())) {
                throw new IllegalArgumentException(
                        "edge references unknown source node: " + edge.fromNode());
            }
            if (!byId.containsKey(edge.toNode())) {
                throw new IllegalArgumentException(
                        "edge references unknown target node: " + edge.toNode());
            }
            String key = edge.toNode() + "/" + edge.toPort();
            if (!seenInputPorts.add(key)) {
                throw new IllegalArgumentException(
                        "two edges target the same input port: " + key);
            }
            incoming.computeIfAbsent(edge.toNode(), k -> new ArrayList<>()).add(edge);
            outgoingTargets.computeIfAbsent(edge.fromNode(), k -> new ArrayList<>()).add(edge.toNode());
        }

        // Kahn's algorithm with deterministic tie-break.
        Map<String, Integer> indegree = new HashMap<>();
        for (LogicNode node : program.nodes()) {
            indegree.put(node.id(), 0);
        }
        for (LogicEdge edge : program.edges()) {
            indegree.merge(edge.toNode(), 1, Integer::sum);
        }

        // TreeMap-backed deque: always pull the lex-smallest ready node id.
        TreeMap<String, Boolean> ready = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                ready.put(entry.getKey(), Boolean.TRUE);
            }
        }

        List<String> ordered = new ArrayList<>(program.nodes().size());
        while (!ready.isEmpty()) {
            String next = ready.pollFirstEntry().getKey();
            ordered.add(next);
            List<String> outgoing = outgoingTargets.getOrDefault(next, List.of());
            for (String targetId : outgoing) {
                int newDeg = indegree.merge(targetId, -1, Integer::sum);
                if (newDeg == 0) {
                    ready.put(targetId, Boolean.TRUE);
                }
            }
        }
        if (ordered.size() != program.nodes().size()) {
            throw new IllegalArgumentException(
                    "logic program contains a cycle (compiled "
                            + ordered.size() + " of " + program.nodes().size() + " nodes)");
        }

        // Materialise steps with executors + input bindings.
        List<CompiledProgram.Step> steps = new ArrayList<>(ordered.size());
        for (String nodeId : ordered) {
            LogicNode node = byId.get(nodeId);
            LogicNodeExecutor executor = LogicNodeKind.executor(node.kind())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "unknown node kind: " + node.kind() + " (node " + nodeId + ")"));
            List<LogicEdge> in = incoming.getOrDefault(nodeId, List.of());
            List<CompiledProgram.InputBinding> bindings = new ArrayList<>(in.size());
            // Sort bindings by toPort for deterministic execution-time iteration.
            List<LogicEdge> sortedIn = new ArrayList<>(in);
            sortedIn.sort((a, b) -> a.toPort().compareTo(b.toPort()));
            for (LogicEdge edge : sortedIn) {
                bindings.add(new CompiledProgram.InputBinding(
                        edge.toPort(), edge.fromNode(), edge.fromPort()));
            }
            steps.add(new CompiledProgram.Step(node, executor, bindings));
        }
        return new CompiledProgram(program.name(), steps);
    }
}
