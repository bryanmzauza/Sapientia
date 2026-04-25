package dev.brmz.sapientia.core.logic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import dev.brmz.sapientia.api.logic.LogicEdge;
import dev.brmz.sapientia.api.logic.LogicNode;
import dev.brmz.sapientia.api.logic.LogicProgram;
import dev.brmz.sapientia.api.logic.LogicValue;
import org.junit.jupiter.api.Test;

/** Round-trip tests for {@link LogicYaml} (T-302 / 1.3.0). */
final class LogicYamlTest {

    private final LogicYaml yaml = new LogicYaml();

    @Test
    void roundTripsAllValueTypes() {
        LogicProgram original = new LogicProgram("sample", List.of(
                new LogicNode("c1", "constant", Map.of(
                        "value", LogicValue.ofInt(7))),
                new LogicNode("c2", "constant", Map.of(
                        "value", LogicValue.ofBool(true))),
                new LogicNode("c3", "constant", Map.of(
                        "value", LogicValue.ofString("hello"))),
                new LogicNode("sink", "noop", Map.of())
        ), List.of(
                new LogicEdge("c1", "out", "sink", "i1"),
                new LogicEdge("c2", "out", "sink", "i2"),
                new LogicEdge("c3", "out", "sink", "i3")
        ));

        String text = yaml.dump(original);
        LogicProgram parsed = yaml.parse(text);

        assertThat(parsed.name()).isEqualTo("sample");
        assertThat(parsed.nodes()).hasSize(4);
        assertThat(parsed.edges()).hasSize(3);
        assertThat(parsed.nodes().get(0).params().get("value"))
                .isEqualTo(LogicValue.ofInt(7));
        assertThat(parsed.nodes().get(1).params().get("value"))
                .isEqualTo(LogicValue.ofBool(true));
        assertThat(parsed.nodes().get(2).params().get("value"))
                .isEqualTo(LogicValue.ofString("hello"));
    }
}
