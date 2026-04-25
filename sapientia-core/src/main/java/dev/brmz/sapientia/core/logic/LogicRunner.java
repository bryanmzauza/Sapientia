package dev.brmz.sapientia.core.logic;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import dev.brmz.sapientia.api.logic.LogicValue;
import org.jetbrains.annotations.NotNull;

/**
 * Single-tick executor of a {@link CompiledProgram} (T-302 / 1.3.0).
 *
 * <p>Walks the topologically-ordered steps once, materialising each node's
 * input map by reading the previous nodes' outputs, then dispatches to the
 * resolved {@link LogicNodeExecutor}. The runtime cost is bounded by
 * {@code O(|V|+|E|)} so a program's worst-case tick fits inside the
 * P-004 envelope along with the rest of the per-tick work.
 */
public final class LogicRunner {

    public void run(@NotNull CompiledProgram compiled, @NotNull LogicContext ctx) {
        Map<String, Map<String, LogicValue>> outputsByNode = new HashMap<>();
        for (CompiledProgram.Step step : compiled.steps()) {
            Map<String, LogicValue> inputs = new LinkedHashMap<>();
            for (CompiledProgram.InputBinding binding : step.inputs()) {
                Map<String, LogicValue> upstream = outputsByNode.get(binding.fromNode());
                if (upstream != null) {
                    LogicValue value = upstream.get(binding.fromPort());
                    if (value != null) {
                        inputs.put(binding.toPort(), value);
                    }
                }
            }
            Map<String, LogicValue> outputs = step.executor().execute(
                    step.node().id(), step.node().params(), inputs, ctx);
            if (!outputs.isEmpty()) {
                outputsByNode.put(step.node().id(), outputs);
            }
        }
    }
}
