package dev.brmz.sapientia.core.logic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import dev.brmz.sapientia.api.logic.LogicEdge;
import dev.brmz.sapientia.api.logic.LogicNode;
import dev.brmz.sapientia.api.logic.LogicProgram;
import dev.brmz.sapientia.api.logic.LogicValue;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LogicCompiler} (T-302 / 1.3.0). Validates DAG cycle
 * detection, edge endpoint resolution, port collision rejection and
 * deterministic topological ordering.
 */
final class LogicCompilerTest {

    private final LogicCompiler compiler = new LogicCompiler();

    @Test
    void simpleLinearProgramCompiles() {
        LogicProgram program = new LogicProgram("p", List.of(
                node("a", "constant", Map.of("value", LogicValue.ofInt(1))),
                node("b", "constant", Map.of("value", LogicValue.ofInt(2))),
                node("sum", "add", Map.of()),
                node("out", "log", Map.of("prefix", LogicValue.ofString("r")))
        ), List.of(
                new LogicEdge("a", "out", "sum", "a"),
                new LogicEdge("b", "out", "sum", "b"),
                new LogicEdge("sum", "out", "out", "value")
        ));
        CompiledProgram compiled = compiler.compile(program);
        assertThat(compiled.steps()).hasSize(4);
        // Lexicographic tie-break: a < b among indegree-0 nodes.
        assertThat(compiled.steps().get(0).node().id()).isEqualTo("a");
        assertThat(compiled.steps().get(1).node().id()).isEqualTo("b");
        assertThat(compiled.steps().get(2).node().id()).isEqualTo("sum");
        assertThat(compiled.steps().get(3).node().id()).isEqualTo("out");
    }

    @Test
    void cycleIsRejected() {
        LogicProgram program = new LogicProgram("cycle", List.of(
                node("x", "noop", Map.of()),
                node("y", "noop", Map.of())
        ), List.of(
                new LogicEdge("x", "out", "y", "in"),
                new LogicEdge("y", "out", "x", "in")
        ));
        assertThatThrownBy(() -> compiler.compile(program))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void unknownNodeReferenceIsRejected() {
        LogicProgram program = new LogicProgram("p", List.of(
                node("a", "noop", Map.of())
        ), List.of(
                new LogicEdge("a", "out", "ghost", "in")
        ));
        assertThatThrownBy(() -> compiler.compile(program))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void unknownKindIsRejected() {
        LogicProgram program = new LogicProgram("p", List.of(
                node("a", "definitely-not-a-real-kind", Map.of())
        ), List.of());
        assertThatThrownBy(() -> compiler.compile(program))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown node kind");
    }

    @Test
    void duplicateInputPortIsRejected() {
        LogicProgram program = new LogicProgram("p", List.of(
                node("a", "constant", Map.of("value", LogicValue.ofInt(1))),
                node("b", "constant", Map.of("value", LogicValue.ofInt(2))),
                node("sum", "add", Map.of())
        ), List.of(
                new LogicEdge("a", "out", "sum", "a"),
                new LogicEdge("b", "out", "sum", "a")
        ));
        assertThatThrownBy(() -> compiler.compile(program))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("two edges target the same input port");
    }

    private static LogicNode node(String id, String kind, Map<String, LogicValue> params) {
        return new LogicNode(id, kind, params);
    }
}
