package dev.brmz.sapientia.core.logic;

import java.util.List;

import dev.brmz.sapientia.api.logic.LogicNode;
import org.jetbrains.annotations.NotNull;

/**
 * Result of {@link LogicCompiler#compile} (T-302 / 1.3.0). Holds the original
 * program nodes in topological execution order, paired with their resolved
 * executor and the list of incoming edges that feed each input port.
 */
public record CompiledProgram(@NotNull String name,
                              @NotNull List<Step> steps) {

    public CompiledProgram {
        steps = List.copyOf(steps);
    }

    /** One topologically-ordered step in the compiled program. */
    public record Step(@NotNull LogicNode node,
                       @NotNull LogicNodeExecutor executor,
                       @NotNull List<InputBinding> inputs) {

        public Step {
            inputs = List.copyOf(inputs);
        }
    }

    /** Binds a node input port to the upstream node + output port that feeds it. */
    public record InputBinding(@NotNull String toPort,
                               @NotNull String fromNode,
                               @NotNull String fromPort) {
    }
}
