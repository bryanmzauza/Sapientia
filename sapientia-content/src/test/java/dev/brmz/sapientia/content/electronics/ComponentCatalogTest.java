package dev.brmz.sapientia.content.electronics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** T-422 / 1.6.0: electronics component catalogue invariants. */
class ComponentCatalogTest {

    @Test
    void totalComponentCountIs17() {
        assertThat(Component.values()).hasSize(17);
    }

    @Test
    void allIdBasesAreUnique() {
        Set<String> ids = new HashSet<>();
        for (Component c : Component.values()) {
            assertThat(ids.add(c.idBase()))
                    .as("duplicate idBase %s", c.idBase())
                    .isTrue();
        }
    }

    @Test
    void allComponentsHaveProxyMaterial() {
        for (Component c : Component.values()) {
            assertThat(c.material())
                    .as("component %s material", c.idBase())
                    .isNotNull();
            assertThat(c.idBase()).isNotBlank();
            assertThat(c.displayKeyEn()).startsWith("component.").endsWith(".name");
        }
    }
}
