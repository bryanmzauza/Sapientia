package dev.brmz.sapientia.core.logic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.brmz.sapientia.api.logic.LogicEdge;
import dev.brmz.sapientia.api.logic.LogicNode;
import dev.brmz.sapientia.api.logic.LogicProgram;
import dev.brmz.sapientia.api.logic.LogicValue;
import org.junit.jupiter.api.Test;

/**
 * Behaviour tests for {@link LogicRunner} + {@link LogicCompiler} +
 * the built-in node kinds (T-302 / 1.3.0).
 */
final class LogicRunnerTest {

    private final LogicCompiler compiler = new LogicCompiler();
    private final LogicRunner runner = new LogicRunner();

    @Test
    void constantPlusConstantWritesMemory() {
        LogicProgram program = new LogicProgram("sum", List.of(
                new LogicNode("a", "constant", Map.of("value", LogicValue.ofInt(40))),
                new LogicNode("b", "constant", Map.of("value", LogicValue.ofInt(2))),
                new LogicNode("sum", "add", Map.of()),
                new LogicNode("store", "memory_write",
                        Map.of("key", LogicValue.ofString("answer")))
        ), List.of(
                new LogicEdge("a", "out", "sum", "a"),
                new LogicEdge("b", "out", "sum", "b"),
                new LogicEdge("sum", "out", "store", "value")
        ));
        CompiledProgram compiled = compiler.compile(program);
        LogicContext ctx = new LogicContext("sum", new HashMap<>(), 0L);
        runner.run(compiled, ctx);
        assertThat(ctx.readMemory("answer")).isEqualTo(LogicValue.ofInt(42L));
    }

    @Test
    void compareAndBranchSelectsCorrectInput() {
        LogicProgram program = new LogicProgram("branch", List.of(
                new LogicNode("a", "constant", Map.of("value", LogicValue.ofInt(7))),
                new LogicNode("b", "constant", Map.of("value", LogicValue.ofInt(3))),
                new LogicNode("yes", "constant", Map.of("value", LogicValue.ofString("greater"))),
                new LogicNode("no", "constant", Map.of("value", LogicValue.ofString("not-greater"))),
                new LogicNode("cmp", "compare", Map.of("op", LogicValue.ofString("gt"))),
                new LogicNode("br", "branch", Map.of()),
                new LogicNode("store", "memory_write",
                        Map.of("key", LogicValue.ofString("result")))
        ), List.of(
                new LogicEdge("a", "out", "cmp", "a"),
                new LogicEdge("b", "out", "cmp", "b"),
                new LogicEdge("cmp", "out", "br", "cond"),
                new LogicEdge("yes", "out", "br", "whenTrue"),
                new LogicEdge("no", "out", "br", "whenFalse"),
                new LogicEdge("br", "out", "store", "value")
        ));
        CompiledProgram compiled = compiler.compile(program);
        LogicContext ctx = new LogicContext("branch", new HashMap<>(), 0L);
        runner.run(compiled, ctx);
        assertThat(ctx.readMemory("result")).isEqualTo(LogicValue.ofString("greater"));
    }

    @Test
    void tickCounterReadsContextTick() {
        LogicProgram program = new LogicProgram("tick", List.of(
                new LogicNode("t", "tick_counter", Map.of()),
                new LogicNode("store", "memory_write",
                        Map.of("key", LogicValue.ofString("t")))
        ), List.of(
                new LogicEdge("t", "out", "store", "value")
        ));
        CompiledProgram compiled = compiler.compile(program);
        LogicContext ctx = new LogicContext("tick", new HashMap<>(), 12345L);
        runner.run(compiled, ctx);
        assertThat(ctx.readMemory("t")).isEqualTo(LogicValue.ofInt(12345L));
    }

    @Test
    void logBuffersMessage() {
        LogicProgram program = new LogicProgram("log", List.of(
                new LogicNode("c", "constant", Map.of("value", LogicValue.ofInt(99))),
                new LogicNode("l", "log", Map.of("prefix", LogicValue.ofString("v")))
        ), List.of(
                new LogicEdge("c", "out", "l", "value")
        ));
        CompiledProgram compiled = compiler.compile(program);
        LogicContext ctx = new LogicContext("log", new HashMap<>(), 0L);
        runner.run(compiled, ctx);
        Map<String, String> drained = ctx.drainLog();
        assertThat(drained).containsEntry("l", "v=99");
    }
}
