package dev.brmz.sapientia.core.logic;

import java.util.Map;

import dev.brmz.sapientia.api.logic.LogicValue;
import org.jetbrains.annotations.NotNull;

/**
 * Pure executor for one {@link dev.brmz.sapientia.api.logic.LogicNode} kind
 * (T-302 / 1.3.0). Implementations must be deterministic — given the same
 * {@code params}, {@code inputs}, and {@code ctx} they must always produce the
 * same outputs and the same memory mutations.
 */
@FunctionalInterface
public interface LogicNodeExecutor {

    /**
     * Executes the node once. Returned map is keyed by output port name and is
     * read by downstream edges; an empty map represents a node with no outputs
     * (e.g. {@code log}, {@code memory_write}).
     *
     * @param params static parameters baked into the node
     * @param inputs values arriving at this node's input ports (one per incoming edge)
     * @param ctx    shared execution scratchpad (memory + tick + log buffer)
     */
    @NotNull Map<String, LogicValue> execute(@NotNull String nodeId,
                                             @NotNull Map<String, LogicValue> params,
                                             @NotNull Map<String, LogicValue> inputs,
                                             @NotNull LogicContext ctx);
}
